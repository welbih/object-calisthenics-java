# Contrato público da fachada

Pacote: `br.com.treino.pedido`. Instanciação: `new PedidoService()`.
Sem persistência ou segurança para concorrência. Cada instância inicia suas três
sequências independentes em 1; validações rejeitadas não consomem ids.

```java
public long cadastrarCliente(String nome, String email, String cpf, boolean vip, String rua, String cep, String cidade, String uf);
public void atualizarContato(long clienteId, String email, String cpf);
public String consultarContato(long clienteId);
public long cadastrarProduto(String nome, double vlrUnit, int estoque, String categoria);
public void alterarPreco(long produtoId, double valor);
public void reporEstoque(long produtoId, int qtd);
public int consultarEstoque(long produtoId);
public void desativarProduto(long produtoId);
public long abrirPedido(long clienteId);
public void adicionarItem(long pedidoId, long produtoId, int qtd);
public void removerItem(long pedidoId, long produtoId);
public void aplicarCupom(long pedidoId, String cupom);
public String consultarItens(long pedidoId);
public double calcularSubtotal(long pedidoId);
public String simularPedido(long pedidoId);
public String finalizarPedido(long pedidoId);
public void cancelarPedido(long pedidoId);
public String consultarStatus(long pedidoId);
public String relatorioCliente(long clienteId);

```

## Comportamento

- Cliente: nome aparado; e-mail simples com um @, ponto no domínio e sem espaços;
  CPF apenas 11 dígitos, não repetidos, com dois verificadores válidos e único.
  Atualização valida tudo antes de alterar contato. CPF igual ao do próprio cliente é permitido.
  Rua/cidade não vazias, CEP com oito dígitos. UFs suportadas: SP, RJ, MG, ES,
  PR, SC, RS, BA, PE, AM, PA, GO e DF. Outras UFs são rejeitadas nesta base.
- Produto: nome aparado, preço finito estritamente positivo antes de arredondar,
  estoque inicial não negativo e categoria LIVRO/ELETRONICO/ALIMENTO.
  Preço arredondado HALF_UP a duas casas; 0.004 é aceito e vira 0.00.
  Reposição positiva e sem ultrapassar Integer.MAX_VALUE. Desativação é definitiva.
- Pedido: começa ABERTO e sem itens. Quantidade 1 a 100 por produto, inclusive
  acumulada. Adições repetidas somam quantidade e preservam o primeiro preço;
  remover e readicionar captura o preço atual. A ordem é a de inserção.
  Adição e simulação não reservam estoque. Remover item inexistente lança NoSuchElementException.
- Cupom exato, sensível a maiúsculas: vazio limpa, DEZ, VINTE ou FRETEGRATIS;
  aplicação substitui o anterior. VIP recebe 5% do subtotal, arredondado primeiro;
  DEZ aplica 10% sobre subtotal menos desconto VIP, com novo arredondamento.
  VINTE soma 20 se subtotal original >= 200. Frete usa subtotal original >= 300
  para gratuidade; FRETEGRATIS também zera. Sudeste 10, Sul 15, Centro-Oeste 20,
  Nordeste 25, Norte 30. Total = subtotal - desconto + frete, HALF_UP.
- Simular vazio é permitido e cobra frete. Finalizar vazio é proibido.
  Finalizar valida todos os itens ativos e seus estoques antes de baixar qualquer
  estoque, calcula os valores e muda para FINALIZADO. Uma segunda finalização falha.
- Cancelar ABERTO não altera estoque. Cancelar FINALIZADO repõe todos os itens
  após validar overflow em todos. Cancelar CANCELADO falha. Pedidos fechados
  não admitem adição, remoção ou cupom. Simular fechado retorna os valores
  armazenados: cancelado ainda aberto preserva a última simulação (ou zeros).
- Contato retorna nome|email|cpf. Itens retorna id|nome|quantidade|preço, separados
  por ponto e vírgula, vazio sem itens. Resumo retorna
  STATUS|subtotal=0.00|desconto=0.00|frete=0.00|total=0.00.
  Relatório considera só pedidos FINALIZADOS daquele cliente e itens com qtd >= 2,
  em ordem de abertura/inserção: pedidoId:nome:qtd; (inclui ponto e vírgula final).
- Id ausente: NoSuchElementException. Entrada inválida: IllegalArgumentException.
  Estado inválido, inativo, falta de estoque e overflow ao cancelar:
  IllegalStateException. A ordem de validação da fachada é observável quando há
  múltiplos erros; os testes priorizam entradas com um erro por cenário.

Os textos não têm escape de delimitadores. Valores monetários usam double na
fronteira e na base; não se promete precisão financeira para magnitudes extremas.
Esta limitação também é material para discutir na regra 3, sem alterar os casos
caracterizados. Não é um sistema fiscal nem valida cadastro real de CPF/CEP.
