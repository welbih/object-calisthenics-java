# Treino de Object Calisthenics em Java

Loja em memória para refatoração. O ponto de partida foi deliberadamente escrito
para exercitar as nove regras. Não é uma arquitetura recomendada para produção.

## Requisitos e início

- JDK 17 ou superior, Maven 3.9+, Git e Bash (Linux/macOS ou Git Bash no Windows).
- Python 3.9+ apenas para os indicadores do roteiro.
- A primeira execução do Maven precisa baixar dependências.

```bash
java -version
mvn -version
mvn test
git config --global user.name "Seu Nome"
git config --global user.email "seu-email@example.com"
bash criar-branches.sh
git switch regra-01-um-nivel-indentacao
```

Configure sua identidade real, localmente com `git config user.name` e
`git config user.email` se preferir (depois de `git init -b main`).
O script verifica a identidade efetiva; não inventa autor nem muda configurações.
Execute-o na cópia extraída do ZIP. Ele cria um repositório local se necessário,
faz o commit inicial e cria dez branches com um commit de NOTAS.md em cada uma.
Branches existentes são preservadas, inclusive seus commits de exercício.
A execução exige árvore limpa após o commit inicial e volta à branch original.
Em repositórios existentes, exige main; não faz reset, merge, force nem novo commit nela.
Não execute no meio de outro repositório em que queira compartilhar o histórico:
o projeto extraído deve ser uma pasta de treino independente.

## Estrutura

- `src/main/java/br/com/treino/cliente`: cadastro e endereço.
- `src/main/java/br/com/treino/produto`: catálogo e estoque.
- `src/main/java/br/com/treino/pedido`: fachada, pedido e itens.
- `src/test/java/br/com/treino/pedido`: testes exclusivamente pela fachada.
- `API.md`: assinaturas e comportamento observável da fachada.
- `ROTEIRO.md`: desafios, locais para procurar e verificações medidas.
- `ferramentas/verificar.py`: indicadores lexicais das nove regras.
- `VERIFICACAO.md`: resultados efetivamente executados nesta entrega.

## Contrato e fluxo

A API pública de `PedidoService` é a fronteira estável: ids `long`, quantidades
`int`, valores `double`, flags e textos. Modelos não fazem parte desse contrato.
Não mude os testes, as assinaturas ou o comportamento observável para acomodar
uma refatoração. Os testes não acessam modelos, reflexão, arquivos ou banco.
Não verificam mensagens de exceção. Formatação usa Locale.US, sem relógio,
rede, aleatoriedade ou estado compartilhado entre testes.

Cada regra começa da mesma main, e não da regra anterior. Rode `mvn test`
antes e depois de cada passo, registre decisões em NOTAS.md e faça commits pequenos.
Não faça merge na main. Use `todas-as-regras` para a combinação final.
As branches não contêm soluções. O ZIP não contém histórico Git nem target;
as branches são criadas localmente pelo script.

```bash
git switch main
git status --short
bash criar-branches.sh
git branch --list
python ferramentas/verificar.py
mvn test
```

No Linux/macOS, substitua `python` por `python3` se necessário. Os indicadores
vão falhar propositalmente na base quando chamados com o número da regra.
O exercício aplica as regras ao código de domínio, com exceção da assinatura
primitiva da fachada exigida pelo contrato. Bibliotecas, imports e infraestrutura
devem ser considerados na revisão manual dos indicadores.
