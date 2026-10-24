# Verificação executada — 24/10/2026

## Ambiente

Windows, Git Bash 5.2.37, Git 2.51.2.windows.1, Maven 3.9.9,
Temurin JDK 25.0.4.1. O compilador usou `--release 17`; o bytecode da fachada
foi conferido como major version 61 (Java 17).

## Testes de caracterização

`mvn test` na main da cópia: **91 testes, zero falhas/erros/ignorados**.
Após restaurar todas as mutações, 91 passaram novamente.
Também passaram 91 em cada execução com:

```bash
mvn test -Duser.language=pt -Duser.country=BR
mvn test -Duser.language=en -Duser.country=US
```

Há 6 grupos @Nested e uso de @ParameterizedTest. Os testes importam apenas a
fachada do projeto, não modelos, e não verificam mensagens de exceção.

## Mutações manuais

Cada alteração foi aplicada isoladamente em PedidoService.java na cópia,
compilou e foi detectada por `mvn test`. O original foi usado para iniciar
cada alteração; nenhuma mutação permanece na entrega.

| Mutação | Testes | Falhas de asserção | Erros |
|---|---:|---:|---:|
| frete-limiar | 91 | 1 | 0 |
| cupom-limiar | 91 | 2 | 0 |
| vip-percentual | 91 | 3 | 0 |
| estoque-baixa | 91 | 3 | 1 |
| arredondamento | 91 | 3 | 0 |
| cpf-verificador | 91 | 1 | 0 |
| limite-itens | 91 | 0 | 2 |
| relatorio-filtro | 91 | 2 | 0 |

Substituições exatas, uma ocorrência cada no original:

| Nome | Antes | Depois |
|---|---|---|
| frete-limiar | `sub >= 300` | `sub > 300` |
| cupom-limiar | `sub >= 200` | `sub > 200` |
| vip-percentual | `sub * 0.05` | `sub * 0.04` |
| estoque-baixa | `prod.getEstoque() - item.getQtd()` | `prod.getEstoque() + item.getQtd()` |
| arredondamento | `RoundingMode.HALF_UP` | `RoundingMode.DOWN` |
| cpf-verificador | `pos < 11` | `pos < 10` |
| limite-itens | `qtd <= 0 \|\| qtd > 100` | `qtd <= 0 \|\| qtd >= 100` |
| relatorio-filtro | `item.getQtd() >= 2` | `item.getQtd() > 2` |

Para repetir, faça uma substituição, rode `mvn test`, restaure o arquivo,
rode os testes verdes e só então experimente outra. A mutação de CPF ignora o
segundo verificador; a suíte rejeita a aceitação indevida.

## Script de branches

Em uma cópia independente da árvore de fontes:

1. Sem identidade, recusou fazer o commit inicial (nenhum HEAD criado).
2. Com identidade de teste configurada apenas na cópia, a primeira execução
   de `bash criar-branches.sh` criou main mais 9 regras e todas-as-regras.
3. A segunda execução preservou os mesmos 11 nomes e SHAs das referências.
4. main tem exatamente 1 commit e permaneceu com SHA `a13e6250a85f375e04b07bf4ef0e11405a5a0aea`.
5. Cada uma das 10 branches tem um commit cujo pai é a main, e a diferença
   para a main é exclusivamente NOTAS.md, com as quatro perguntas exigidas.
6. O script voltou à main e a árvore ficou limpa.

Uma falha encontrada na primeira tentativa (comparação de caminhos entre Git
Windows e Bash) foi corrigida antes destas duas execuções válidas.

## Contagens e comandos

`python ferramentas/verificar.py` retornou sucesso e as contagens abaixo.
Cada comando com argumento 01 a 09 foi executado e retornou 1, como esperado
na base ainda sem refatorar. Os números do roteiro foram conferidos contra
esse resultado, as declarações públicas e os campos do código.

```text
Classes: 8
Cidade: 22 linhas, 2 atributos
Cliente: 58 linhas, 6 atributos
Endereco: 31 linhas, 3 atributos
Estado: 22 linhas, 2 atributos
Item: 40 linhas, 4 atributos
Pedido: 79 linhas, 8 atributos
PedidoService: 400 linhas, 6 atributos
Produto: 58 linhas, 6 atributos
Regra 01: 20
Regra 02: 10
Regra 03: 26
Regra 04: 12
Regra 05: 26
Regra 06: 271
Regra 07: 4
Regra 08: 6
Regra 09: 62
```

## Limitações verificadas

- Não havia JDK 17 selecionado no ambiente: compilação para Java 17 foi
  verificada, mas a execução ocorreu no JDK 25, não numa JVM 17.
- O script foi executado em Git Bash no Windows. Não foi executado em Linux,
  macOS ou WSL. O script exige Bash; não é um script PowerShell.
- Maven e Bash precisaram executar fora da restrição inicial do ambiente:
  o cache Maven e a inicialização MSYS foram bloqueados na primeira tentativa.
  Com a permissão de execução, as dependências foram resolvidas e os testes rodaram.
- São oito mutações escolhidas manualmente, sem PIT e sem afirmar cobertura
  exaustiva ou escore de mutação. Não houve medição JaCoCo.
- Indicadores são lexicais; a regra 4 e chamadas de biblioteca na regra 5
  exigem revisão das ocorrências restantes, não apenas resultado zero.
- Loja em memória, sem persistência/concorrência, sem escape de delimitadores,
  UFs limitadas e precisão de double em magnitudes extremas: detalhes em API.md.

## Integridade da entrega

O ZIP foi inspecionado: nenhuma entrada target/ ou .git/. Os bytes dos fontes
e testes empacotados foram comparados aos da cópia testada, e ROTEIRO.md externo
é idêntico ao interno. Logs e resumo estruturado estão em verificacao/.
