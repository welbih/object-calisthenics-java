# Roteiro de treino — Object Calisthenics

## Contrato do jogo

Preserve todas as assinaturas públicas de PedidoService (API.md), o formato dos
textos, os tipos de exceção e o comportamento dos testes. Não edite os testes.
Modelos e métodos privados podem desaparecer. Não faça merge na main. Cada branch
isolada nasce da mesma main e recebe só sua regra; mudanças de apoio são permitidas,
mas anote quando também ajudarem outra regra. Não há soluções prontas.

A regra 3 tem exceção explícita para a fronteira pública primitiva da fachada.
Conversões devem acontecer na borda. A regra 9 trata dos acessores de modelos;
consultarEstoque e consultarStatus são consultas de aplicação protegidas pelo contrato.
Para a regra 7, use 50 linhas físicas por classe como meta de treino, contando
brancos e imports; não comprima código para atingir a meta. Para a regra 8, limite
a dois campos de instância por classe, sem esconder estado em arrays ou mapas.

## Rede de segurança

Os 91 casos executados (incluindo invocações parametrizadas) protegem:

1. Normalização do nome, contato atualizável, e-mail, verificadores e unicidade de
   CPF, endereço obrigatório, CEP numérico e a lista explícita de UFs.
2. Preços positivos e finitos, HALF_UP, inclusive preço positivo que vira zero,
   três categorias, estoque inicial, reposição e overflow.
3. Ids independentes e sequenciais, instâncias isoladas e ids ausentes em toda API.
4. Ordem dos itens, agrupamento, preço capturado, remoção/readição, limite 1–100,
   ausência de reserva e rejeição de produto inativo.
5. VIP 5%, DEZ sobre a base após VIP, VINTE a partir de 200, limiar do frete em
   300, arredondamento intermediário, cinco tarifas regionais e FRETEGRATIS.
6. Substituição/limpeza e rejeição de cupom, simulação vazia e formatos de texto.
7. Fechamento não vazio, estoque exato, disputa entre pedidos, validação de todos
   os itens antes da baixa e recusa de produto desativado após adição.
8. Estados ABERTO/FINALIZADO/CANCELADO, proibição de edição e repetição de ações,
   devolução única e cancelamento atômico quando a devolução causaria overflow.
9. Relatório por cliente, só finalizados, só quantidades >= 2 e ordem estável.

Consulte API.md para detalhes e limitações do contrato. A suíte não prova ausência
de todos os defeitos; as mutações manuais da entrega são uma amostra, não um escore PIT.

## Como medir

Execute na raiz, com Python 3 (`python3` se esse for o nome no seu sistema):

```bash
python ferramentas/verificar.py
mvn test
```

Foram medidos 8 arquivos/classes de produção. Linhas físicas/atributos:
Cidade 22/2; Cliente 58/6; Endereco 31/3; Estado 22/2; Item 40/4;
Pedido 79/8; PedidoService 400/6; Produto 58/6.

Os comandos `python ferramentas/verificar.py NN` abaixo foram executados na base:
todos retornam 1 porque ainda há indicadores. Após refatorar, espere 0 onde o
critério exige zerar e faça a revisão manual descrita para bibliotecas e coleções.
São buscas lexicais, não um analisador Java: podem contar
imports, chamadas de biblioteca e comentários, e perder casos se a formatação mudar.
Não altere o verificador para obter verde. Justifique exceções de biblioteca em
NOTAS.md; zero indicadores sozinho nunca prova conformidade.

## 1. Um nível de indentação por método

Branch: `regra-01-um-nivel-indentacao`.
A regra pede no máximo um nível de controle dentro de cada método.
Procure em PedidoService.adicionarItem, atualizarContato, finalizarPedido,
relatorioCliente e vldCpf. Há 20 linhas de if/for/while com pelo menos 12 espaços
em produção. relatorioCliente combina laço de pedidos, filtro de cliente,
filtro de estado, laço de itens e filtro de quantidade.

Ataque: extraia primeiro a decisão mais interna; dê nome à intenção; depois
extraia a iteração. Preserve ordem, retorno antecipado e atomicidade do fechamento.
Armadilha: apenas mover o bloco para outro método igualmente aninhado não resolve.
Não troque loops por streams complexos para esconder níveis.

Pronto: cada método tem só um nível de controle, revisão de todos os métodos e
testes verdes. Indicador a zerar:

```bash
python ferramentas/verificar.py 01
```

## 2. Não usar else

Branch: `regra-02-sem-else`.
Troque alternativas por retornos antecipados, objetos de política ou polimorfismo.
Há 10 tokens else em PedidoService, nas cadeias de cadastrarCliente e
simularPedido e no tratamento de inativos de finalizarPedido.

Ataque: comece por guard clauses do fechamento; extraia a classificação de região;
separe as políticas de cupom/frete mantendo seus limites. Armadilha: uma coleção
de if independentes pode executar mais de um desconto ou sobrescrever frete.
Não substitua else por ternários ou switch só para escapar da busca.

Pronto: nenhuma alternativa else/ternário disfarçada no domínio, decisões claras
e testes verdes. Comando:

```bash
python ferramentas/verificar.py 02
```

## 3. Encapsular primitivos e Strings

Branch: `regra-03-encapsular-primitivos-strings`.
Crie tipos que concentrem significado, validação e operações. Os modelos possuem
26 campos privados primitivos/String; há ainda números e textos locais na fachada.
Procure Cliente (email/cpf), Produto (vlrUnit/estoque), Item (qtd) e
Pedido (status/cupom/valores), além de vldCpf, arred e atualizarContato.

Ataque: introduza CPF e Email preservando validações; introduza Quantidade e
Dinheiro; converta na borda da fachada; migre estados e cupom. Armadilha: mudar
arredondamento intermediário, passar a aceitar CPF pontuado ou rejeitar 0.004
altera o contrato. Não torne público um tipo de domínio na fachada.

Pronto: conceitos internos têm tipos e operações próprios; nenhuma validação
duplicada; fronteira original e testes preservados. O comando mede apenas campos
de modelos, portanto revise também parâmetros e variáveis internas:

```bash
python ferramentas/verificar.py 03
```

## 4. Coleções de primeira classe

Branch: `regra-04-colecoes-primeira-classe`.
Uma classe que encapsula coleção dedica-se àquela coleção e suas operações.
Há 12 ocorrências dos tipos List/Map/ArrayList/LinkedHashMap/Iterator, incluindo
imports. Pedido.itens é List crua; PedidoService.clis/prods/peds são três mapas
crus. Procure adicionarItem, removerItem, calcularSubtotal e relatorioCliente.

Ataque: tipifique uma coleção; encapsule itens com busca, soma e remoção; crie
catálogos por assunto; retire exposições das coleções. Armadilhas: perder ordem
de LinkedHashMap/ArrayList, expor lista mutável ou mudar a captura do preço.

Pronto: coleções só existem dentro de suas classes dedicadas e não escapam;
testes verdes. Este indicador não deve necessariamente zerar: a implementação
dos wrappers e seus imports ainda usa coleções. Revise cada ocorrência restante
e registre a justificativa; em wrappers, não adicione outros campos de negócio.

```bash
python ferramentas/verificar.py 04
```

## 5. Um ponto por linha

Branch: `regra-05-um-ponto-por-linha`.
Evite navegar pela estrutura de outros objetos; peça comportamento ao colaborador.
Há 26 pares lexicais de chamadas encadeadas em produção. Procure simularPedido,
especialmente ped.getCliente().getEndereco().getCidade().getEstado().getRegiao(),
e expressões como ped.getItens().add e ped.getStatus().equals.

Ataque: identifique a pergunta real (tarifa de entrega, cliente VIP, estado do
pedido); mova a resposta para quem possui o dado; remova a navegação. Armadilha:
quebrar a cadeia em várias variáveis locais cumpre a sintaxe mas mantém o acoplamento.

Pronto: não há navegação de objetos de domínio e testes verdes. Revise os 26
pares, pois BigDecimal e streams de biblioteca também podem ser contados.
Métodos novos devem representar intenção, não só delegar getX().getY().

```bash
python ferramentas/verificar.py 05
```

## 6. Não abreviar

Branch: `regra-06-sem-abreviacoes`.
Nomes devem comunicar intenção sem exigir tradução mental. Há 271 ocorrências
do vocabulário abreviado listado no verificador, incluindo usos e declarações.
Procure qtd/vlrUnit em Item/Produto e cli/prod/ped/desc/sub/reg/vldCpf na fachada.

Ataque: renomeie variáveis locais; renomeie campos e acessores internos; renomeie
métodos privados segundo sua intenção. Armadilha: alterar nomes públicos da
fachada quebra o contrato. Os nomes de parâmetros podem ser esclarecidos sem
mudar a assinatura Java. CPF, CEP, UF e id são termos de domínio aceitos.

Pronto: sem abreviações ambíguas, revisão de nomes além do dicionário e testes verdes:

```bash
python ferramentas/verificar.py 06
```

## 7. Manter entidades pequenas

Branch: `regra-07-entidades-pequenas`.
Mantenha classes com responsabilidade clara e métodos curtos. A meta local é
50 linhas físicas por classe. Há 4 acima disso: PedidoService 400, Pedido 79,
Cliente 58 e Produto 58. A fachada reúne 19 métodos públicos de negócio.

Ataque: extraia cadastro, catálogo, cálculo, estoque e ciclo do pedido; deixe a
fachada só coordenar mantendo suas assinaturas. Divida modelos por coesão.
Armadilhas: criar uma classe Util gigante, comprimir linhas ou distribuir um
único procedimento em classes que só trocam dados. Revise métodos com mais de
10 linhas como sinal de alerta, sem transformar esse número em regra absoluta.

Pronto: todas as classes <= 50 linhas sem compressão, responsabilidades claras
e testes verdes. O comando mede linhas físicas de arquivos Java (uma classe por arquivo):

```bash
python ferramentas/verificar.py 07
```

## 8. No máximo dois atributos de instância

Branch: `regra-08-dois-atributos-por-classe`.
Agrupe estado que evolui junto e distribua responsabilidades. Há 6 classes com
mais de dois campos: Cliente 6, Endereco 3, Item 4, Pedido 8, PedidoService 6,
Produto 6. Cidade e Estado já têm 2.

Ataque: agrupe identificação/contato/endereço; separe posição de estoque de
descrição/preço; encapsule composição e valores do pedido; extraia catálogos da
fachada. Armadilha: colocar tudo em Object[], Map ou uma lista de propriedades
esconde os atributos e enfraquece o modelo. Não use campos estáticos para fugir do limite.

Pronto: <= 2 campos de instância por classe, objetos com sentido, sem esconder
estado e testes verdes. Revise novos tipos e mantenha uma classe por arquivo:

```bash
python ferramentas/verificar.py 08
```

## 9. Não usar getters/setters

Branch: `regra-09-sem-getters-setters`.
Peça ao objeto que faça algo, em vez de buscar seus dados e devolvê-los alterados.
Há 62 declarações públicas get/set nos sete modelos. Pedido tem 16, Cliente e
Produto têm 12 cada. Procure adicionarItem, finalizarPedido, cancelarPedido,
simularPedido e o padrão prod.setEstoque(prod.getEstoque() - item.getQtd()).

Ataque: introduza baixar/devolver estoque e adicionar quantidade; mova cálculos
para os responsáveis; substitua acessores por comandos e consultas de intenção.
Armadilhas: simplesmente renomear getEstoque para estoque ou retornar o modelo
inteiro por uma consulta. Preserve validação em duas fases para não perder atomicidade.

Pronto: sem acessores de estado nos modelos, comportamento encapsulado, fachada
primitiva preservada e testes verdes. Além da busca, revise exposições com outros nomes:

```bash
python ferramentas/verificar.py 09
```

## Desafio final

Na `todas-as-regras`, ordem sugerida: 6 (nomes), 3 (tipos), 4 (coleções),
9 (comportamento), 5 (colaboração), 2 (alternativas), 1 (níveis), 8 (estado),
7 (tamanho). Reavalie as anteriores a cada extração. Termine com `mvn test`,
`python ferramentas/verificar.py` e revisão manual dos nove critérios; registre
as exceções e o que faria diferente em produção no NOTAS.md.
