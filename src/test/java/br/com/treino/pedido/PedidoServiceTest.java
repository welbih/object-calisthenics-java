package br.com.treino.pedido;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.NoSuchElementException;
import static org.junit.jupiter.api.Assertions.*;

class PedidoServiceTest {
    private PedidoService loja;
    private long cliente;
    private long produto;
    private long pedido;

    @BeforeEach
    void preparar() {
        loja = new PedidoService();
        cliente = cadastrar(false, "SP");
        produto = loja.cadastrarProduto("Livro", 50, 100, "LIVRO");
        pedido = loja.abrirPedido(cliente);
    }

    private long cadastrar(boolean vip, String uf) {
        return loja.cadastrarCliente(" Ana ", "ana@exemplo.com", "52998224725", vip,
                "Rua A", "01001000", "Cidade", uf);
    }

    private String simular(double preco, boolean vip, String cupom, String uf) {
        loja = new PedidoService();
        long cli = cadastrar(vip, uf);
        long prod = loja.cadastrarProduto("P", preco, 1, "LIVRO");
        long ped = loja.abrirPedido(cli);
        loja.adicionarItem(ped, prod, 1);
        loja.aplicarCupom(ped, cupom);
        return loja.simularPedido(ped);
    }

    @Nested
    class Cadastro {
        @Test void normalizaNomeEAtualizaContato() {
            assertEquals("Ana|ana@exemplo.com|52998224725", loja.consultarContato(cliente));
            loja.atualizarContato(cliente, "nova@exemplo.com", "11144477735");
            assertEquals("Ana|nova@exemplo.com|11144477735", loja.consultarContato(cliente));
            loja.atualizarContato(cliente, "nova@exemplo.com", "11144477735");
        }
        @ParameterizedTest @NullAndEmptySource @ValueSource(strings={" ", "a", "a@b", "a b@c.com", "a@@b.com"})
        void rejeitaEmail(String email) {
            assertThrows(IllegalArgumentException.class, () -> loja.cadastrarCliente("A", email, "11144477735", false, "R", "01001000", "C", "SP"));
            assertThrows(IllegalArgumentException.class, () -> loja.atualizarContato(cliente, email, "11144477735"));
            assertEquals("Ana|ana@exemplo.com|52998224725", loja.consultarContato(cliente));
        }
        @ParameterizedTest @NullAndEmptySource @ValueSource(strings={"11111111111", "52998224724", "52998224715", "529.982.247-25", "123", "abcdefghijk"})
        void rejeitaCpf(String cpf) {
            assertThrows(IllegalArgumentException.class, () -> loja.cadastrarCliente("A", "a@b.com", cpf, false, "R", "01001000", "C", "SP"));
            assertThrows(IllegalArgumentException.class, () -> loja.atualizarContato(cliente, "a@b.com", cpf));
        }
        @Test void rejeitaDuplicidadeSemConsumirId() {
            assertThrows(IllegalArgumentException.class, () -> cadastrar(false, "RJ"));
            long outro = loja.cadastrarCliente("B", "b@b.com", "11144477735", false, "R", "01001000", "C", "RJ");
            assertEquals(2, outro);
            assertThrows(IllegalArgumentException.class, () -> loja.atualizarContato(outro, "b@b.com", "52998224725"));
        }
        @ParameterizedTest @NullAndEmptySource @ValueSource(strings={" "})
        void rejeitaNomeRuaCidade(String texto) {
            assertThrows(IllegalArgumentException.class, () -> loja.cadastrarCliente(texto, "a@b.com", "11144477735", false, "R", "01001000", "C", "SP"));
            assertThrows(IllegalArgumentException.class, () -> loja.cadastrarCliente("A", "a@b.com", "11144477735", false, texto, "01001000", "C", "SP"));
            assertThrows(IllegalArgumentException.class, () -> loja.cadastrarCliente("A", "a@b.com", "11144477735", false, "R", "01001000", texto, "SP"));
        }
        @ParameterizedTest @NullAndEmptySource @ValueSource(strings={"123", "01001-000", "abcdefgh"})
        void rejeitaCep(String cep) {
            assertThrows(IllegalArgumentException.class, () -> loja.cadastrarCliente("A", "a@b.com", "11144477735", false, "R", cep, "C", "SP"));
        }
        @ParameterizedTest @NullAndEmptySource @ValueSource(strings={"sp", "XX", "AC"})
        void rejeitaUf(String uf) {
            assertThrows(IllegalArgumentException.class, () -> loja.cadastrarCliente("A", "a@b.com", "11144477735", false, "R", "01001000", "C", uf));
        }
    }

    @Nested
    class Catalogo {
        @ParameterizedTest @ValueSource(doubles={0,-1,Double.NaN,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY})
        void rejeitaPreco(double preco) {
            assertThrows(IllegalArgumentException.class, () -> loja.cadastrarProduto("P", preco, 1, "LIVRO"));
            assertThrows(IllegalArgumentException.class, () -> loja.alterarPreco(produto, preco));
        }
        @ParameterizedTest @NullAndEmptySource @ValueSource(strings={" "})
        void rejeitaNome(String nome) {
            assertThrows(IllegalArgumentException.class, () -> loja.cadastrarProduto(nome, 1, 1, "LIVRO"));
        }
        @ParameterizedTest @NullAndEmptySource @ValueSource(strings={"livro", "OUTRO"})
        void rejeitaCategoria(String categoria) {
            assertThrows(IllegalArgumentException.class, () -> loja.cadastrarProduto("P", 1, 1, categoria));
        }
        @ParameterizedTest @ValueSource(strings={"LIVRO", "ELETRONICO", "ALIMENTO"})
        void aceitaCategorias(String categoria) {
            assertEquals(0, loja.consultarEstoque(loja.cadastrarProduto("P", 1, 0, categoria)));
        }
        @Test void reposicaoELimites() {
            assertThrows(IllegalArgumentException.class, () -> loja.cadastrarProduto("P", 1, -1, "LIVRO"));
            assertThrows(IllegalArgumentException.class, () -> loja.reporEstoque(produto, 0));
            assertThrows(IllegalArgumentException.class, () -> loja.reporEstoque(produto, -1));
            loja.reporEstoque(produto, 2);
            assertEquals(102, loja.consultarEstoque(produto));
            long max = loja.cadastrarProduto("M", 1, Integer.MAX_VALUE, "LIVRO");
            assertThrows(IllegalArgumentException.class, () -> loja.reporEstoque(max, 1));
            assertEquals(Integer.MAX_VALUE, loja.consultarEstoque(max));
        }
        @Test void precoPequenoArredondaParaZero() {
            long p = loja.cadastrarProduto("P", .004, 1, "LIVRO");
            loja.adicionarItem(pedido, p, 1);
            assertEquals("2|P|1|0.00", loja.consultarItens(pedido));
        }
    }

    @Nested
    class Itens {
        @Test void capturaPrecoNomeAgrupaEMantemOrdem() {
            loja.adicionarItem(pedido, produto, 1);
            loja.alterarPreco(produto, 99);
            loja.adicionarItem(pedido, produto, 2);
            long outro = loja.cadastrarProduto(" Outro ", 1.005, 1, "ALIMENTO");
            loja.adicionarItem(pedido, outro, 1);
            assertEquals("1|Livro|3|50.00;2|Outro|1|1.01", loja.consultarItens(pedido));
            assertEquals(151.01, loja.calcularSubtotal(pedido), .000001);
            assertEquals(100, loja.consultarEstoque(produto));
            loja.removerItem(pedido, produto);
            assertEquals("2|Outro|1|1.01", loja.consultarItens(pedido));
            loja.adicionarItem(pedido, produto, 1);
            assertEquals("2|Outro|1|1.01;1|Livro|1|99.00", loja.consultarItens(pedido));
        }
        @ParameterizedTest @ValueSource(ints={-1,0,101,Integer.MAX_VALUE})
        void rejeitaQuantidade(int qtd) {
            assertThrows(IllegalArgumentException.class, () -> loja.adicionarItem(pedido, produto, qtd));
            assertEquals("", loja.consultarItens(pedido));
        }
        @Test void limiteAcumulado() {
            loja.adicionarItem(pedido, produto, 99);
            loja.adicionarItem(pedido, produto, 1);
            assertThrows(IllegalArgumentException.class, () -> loja.adicionarItem(pedido, produto, 1));
            assertEquals("1|Livro|100|50.00", loja.consultarItens(pedido));
        }
        @Test void rejeitaInativoEItemAusente() {
            loja.desativarProduto(produto);
            assertThrows(IllegalStateException.class, () -> loja.adicionarItem(pedido, produto, 1));
            assertThrows(NoSuchElementException.class, () -> loja.removerItem(pedido, produto));
        }
    }

    @Nested
    class Calculo {
        @ParameterizedTest @CsvSource({"SP,10", "RJ,10", "MG,10", "ES,10", "PR,15", "SC,15", "RS,15", "GO,20", "DF,20", "BA,25", "PE,25", "AM,30", "PA,30"})
        void fretePorUf(String uf, int frete) {
            assertEquals("ABERTO|subtotal=50.00|desconto=0.00|frete="+frete+".00|total="+(50+frete)+".00", simular(50,false,"",uf));
        }
        @ParameterizedTest @CsvSource({
            "199.99,false,VINTE,0.00,10.00,209.99",
            "200,false,VINTE,20.00,10.00,190.00",
            "299.99,false,DEZ,30.00,10.00,279.99",
            "300,false,DEZ,30.00,0.00,270.00",
            "200,true,VINTE,30.00,10.00,180.00",
            "100,true,DEZ,14.50,10.00,95.50",
            "1.10,true,DEZ,0.16,10.00,10.94",
            "50,false,FRETEGRATIS,0.00,0.00,50.00"})
        void descontosELimites(double preco, boolean vip, String cupom, String desc, String frete, String total) {
            String sub = String.format(java.util.Locale.US, "%.2f", preco);
            assertEquals("ABERTO|subtotal="+sub+"|desconto="+desc+"|frete="+frete+"|total="+total, simular(preco,vip,cupom,"SP"));
        }
        @Test void vazioECupomSubstituido() {
            assertEquals("ABERTO|subtotal=0.00|desconto=0.00|frete=10.00|total=10.00", loja.simularPedido(pedido));
            loja.adicionarItem(pedido, produto, 1);
            loja.aplicarCupom(pedido, "DEZ");
            loja.aplicarCupom(pedido, "");
            assertEquals("ABERTO|subtotal=50.00|desconto=0.00|frete=10.00|total=60.00", loja.simularPedido(pedido));
        }
        @ParameterizedTest @NullSource @ValueSource(strings={"dez", "INVALIDO", " "})
        void rejeitaCupom(String cupom) {
            assertThrows(IllegalArgumentException.class, () -> loja.aplicarCupom(pedido, cupom));
        }
    }

    @Nested
    class Ciclo {
        @Test void finalizaECancelaUmaVez() {
            loja.adicionarItem(pedido, produto, 2);
            String fechado = loja.finalizarPedido(pedido);
            assertEquals("FINALIZADO|subtotal=100.00|desconto=0.00|frete=10.00|total=110.00", fechado);
            assertEquals(98, loja.consultarEstoque(produto));
            assertEquals(fechado, loja.simularPedido(pedido));
            assertEquals("1:Livro:2;", loja.relatorioCliente(cliente));
            assertThrows(IllegalStateException.class, () -> loja.finalizarPedido(pedido));
            loja.cancelarPedido(pedido);
            assertEquals(100, loja.consultarEstoque(produto));
            assertEquals("CANCELADO", loja.consultarStatus(pedido));
            assertEquals("", loja.relatorioCliente(cliente));
            assertThrows(IllegalStateException.class, () -> loja.cancelarPedido(pedido));
            assertEquals(100, loja.consultarEstoque(produto));
        }
        @Test void fechamentoAtomicoEEstoqueExato() {
            long outro = loja.cadastrarProduto("Outro", 10, 0, "LIVRO");
            loja.adicionarItem(pedido, produto, 100);
            loja.adicionarItem(pedido, outro, 1);
            assertThrows(IllegalStateException.class, () -> loja.finalizarPedido(pedido));
            assertEquals(100, loja.consultarEstoque(produto));
            assertEquals("ABERTO", loja.consultarStatus(pedido));
            loja.reporEstoque(outro, 1);
            loja.finalizarPedido(pedido);
            assertEquals(0, loja.consultarEstoque(produto));
            assertEquals(0, loja.consultarEstoque(outro));
        }
        @Test void doisPedidosDisputamEstoque() {
            long outro = loja.abrirPedido(cliente);
            loja.adicionarItem(pedido, produto, 100);
            loja.adicionarItem(outro, produto, 1);
            loja.finalizarPedido(pedido);
            assertThrows(IllegalStateException.class, () -> loja.finalizarPedido(outro));
            assertEquals(0, loja.consultarEstoque(produto));
        }
        @Test void cancelamentoAtomicoContraOverflow() {
            long outro = loja.cadastrarProduto("Outro", 1, 1, "LIVRO");
            loja.adicionarItem(pedido, produto, 1);
            loja.adicionarItem(pedido, outro, 1);
            loja.finalizarPedido(pedido);
            loja.reporEstoque(outro, Integer.MAX_VALUE);
            assertThrows(IllegalStateException.class, () -> loja.cancelarPedido(pedido));
            assertEquals(99, loja.consultarEstoque(produto));
            assertEquals("FINALIZADO", loja.consultarStatus(pedido));
        }
        @Test void vazioEInativoNaoFinalizam() {
            assertThrows(IllegalStateException.class, () -> loja.finalizarPedido(pedido));
            loja.adicionarItem(pedido, produto, 1);
            loja.desativarProduto(produto);
            assertThrows(IllegalStateException.class, () -> loja.finalizarPedido(pedido));
            assertEquals(100, loja.consultarEstoque(produto));
        }
        @ParameterizedTest @ValueSource(strings={"FINALIZADO", "CANCELADO"})
        void estadosBloqueiamEdicao(String status) {
            loja.adicionarItem(pedido, produto, 1);
            if (status.equals("FINALIZADO")) loja.finalizarPedido(pedido);
            else loja.cancelarPedido(pedido);
            assertThrows(IllegalStateException.class, () -> loja.adicionarItem(pedido, produto, 1));
            assertThrows(IllegalStateException.class, () -> loja.removerItem(pedido, produto));
            assertThrows(IllegalStateException.class, () -> loja.aplicarCupom(pedido, "DEZ"));
            assertThrows(IllegalStateException.class, () -> loja.finalizarPedido(pedido));
        }
        @Test void cancelaAbertoSemRepor() {
            loja.adicionarItem(pedido, produto, 2);
            loja.cancelarPedido(pedido);
            assertEquals(100, loja.consultarEstoque(produto));
            assertEquals("CANCELADO|subtotal=100.00|desconto=0.00|frete=0.00|total=0.00", loja.simularPedido(pedido));
        }
        @Test void relatorioFiltraClienteEstadoEQuantidade() {
            long outroCliente = loja.cadastrarCliente("B", "b@b.com", "11144477735", false,"R","01001000","C","SP");
            long outroPedido = loja.abrirPedido(outroCliente);
            loja.adicionarItem(outroPedido, produto, 2);
            loja.finalizarPedido(outroPedido);
            loja.adicionarItem(pedido, produto, 1);
            assertEquals("", loja.relatorioCliente(cliente));
            loja.finalizarPedido(pedido);
            assertEquals("", loja.relatorioCliente(cliente));
            assertEquals("2:Livro:2;", loja.relatorioCliente(outroCliente));
        }
    }

    @Nested
    class Identificadores {
        @Test void ausentesLancamTipoExato() {
            assertThrowsExactly(NoSuchElementException.class, () -> loja.abrirPedido(999));
            assertThrowsExactly(NoSuchElementException.class, () -> loja.consultarContato(999));
            assertThrowsExactly(NoSuchElementException.class, () -> loja.atualizarContato(999,"a@b.com","11144477735"));
            assertThrowsExactly(NoSuchElementException.class, () -> loja.relatorioCliente(999));
            assertThrowsExactly(NoSuchElementException.class, () -> loja.consultarEstoque(999));
            assertThrowsExactly(NoSuchElementException.class, () -> loja.alterarPreco(999, 1));
            assertThrowsExactly(NoSuchElementException.class, () -> loja.reporEstoque(999, 1));
            assertThrowsExactly(NoSuchElementException.class, () -> loja.desativarProduto(999));
            assertThrowsExactly(NoSuchElementException.class, () -> loja.adicionarItem(pedido,999,1));
            assertThrowsExactly(NoSuchElementException.class, () -> loja.adicionarItem(999,produto,1));
            assertThrowsExactly(NoSuchElementException.class, () -> loja.removerItem(999,produto));
            assertThrowsExactly(NoSuchElementException.class, () -> loja.aplicarCupom(999,"DEZ"));
            assertThrowsExactly(NoSuchElementException.class, () -> loja.consultarItens(999));
            assertThrowsExactly(NoSuchElementException.class, () -> loja.calcularSubtotal(999));
            assertThrowsExactly(NoSuchElementException.class, () -> loja.simularPedido(999));
            assertThrowsExactly(NoSuchElementException.class, () -> loja.finalizarPedido(999));
            assertThrowsExactly(NoSuchElementException.class, () -> loja.cancelarPedido(999));
            assertThrowsExactly(NoSuchElementException.class, () -> loja.consultarStatus(999));
        }
        @Test void instanciasIsoladasESequenciasIndependentes() {
            assertEquals(1, cliente);
            assertEquals(1, produto);
            assertEquals(1, pedido);
            PedidoService outra = new PedidoService();
            assertThrows(NoSuchElementException.class, () -> outra.consultarStatus(pedido));
            assertEquals(2, loja.abrirPedido(cliente));
        }
    }
}
