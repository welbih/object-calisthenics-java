package br.com.treino.pedido;

import br.com.treino.cliente.*;
import br.com.treino.produto.Produto;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

public class PedidoService {
    private Map clis = new LinkedHashMap();
    private Map prods = new LinkedHashMap();
    private Map peds = new LinkedHashMap();
    private long seqCli = 1;
    private long seqProd = 1;
    private long seqPed = 1;

    public long cadastrarCliente(String nome, String email, String cpf, boolean vip,
                                  String rua, String cep, String cidade, String uf) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException();
        }
        if (email == null || !email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
            throw new IllegalArgumentException();
        }
        if (!vldCpf(cpf)) {
            throw new IllegalArgumentException();
        }
        if (rua == null || rua.isBlank() || cidade == null || cidade.isBlank()) {
            throw new IllegalArgumentException();
        }
        if (cep == null || !cep.matches("[0-9]{8}")) {
            throw new IllegalArgumentException();
        }
        if (uf == null || !Arrays.asList("SP", "RJ", "MG", "ES", "PR", "SC", "RS", "BA", "PE", "AM", "PA", "GO", "DF").contains(uf)) {
            throw new IllegalArgumentException();
        }
        for (Object obj : clis.values()) {
            Cliente cli = (Cliente) obj;
            if (cli.getCpf().equals(cpf)) {
                throw new IllegalArgumentException();
            }
        }
        Estado est = new Estado();
        est.setSigla(uf);
        if (Arrays.asList("SP", "RJ", "MG", "ES").contains(uf)) {
            est.setRegiao("SUDESTE");
        } else if (Arrays.asList("PR", "SC", "RS").contains(uf)) {
            est.setRegiao("SUL");
        } else if (Arrays.asList("BA", "PE").contains(uf)) {
            est.setRegiao("NORDESTE");
        } else if (Arrays.asList("AM", "PA").contains(uf)) {
            est.setRegiao("NORTE");
        } else {
            est.setRegiao("CENTRO-OESTE");
        }
        Cidade cid = new Cidade();
        cid.setNome(cidade);
        cid.setEstado(est);
        Endereco end = new Endereco();
        end.setRua(rua);
        end.setCep(cep);
        end.setCidade(cid);
        Cliente cli = new Cliente();
        cli.setId(seqCli++);
        cli.setNome(nome.trim());
        cli.setEmail(email);
        cli.setCpf(cpf);
        cli.setVip(vip);
        cli.setEndereco(end);
        clis.put(cli.getId(), cli);
        return cli.getId();
    }

    public void atualizarContato(long clienteId, String email, String cpf) {
        Cliente cli = cli(clienteId);
        if (email == null || !email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
            throw new IllegalArgumentException();
        }
        if (!vldCpf(cpf)) {
            throw new IllegalArgumentException();
        }
        for (Object obj : clis.values()) {
            Cliente outro = (Cliente) obj;
            if (outro.getId() != clienteId) {
                if (outro.getCpf().equals(cpf)) {
                    throw new IllegalArgumentException();
                }
            }
        }
        cli.setEmail(email);
        cli.setCpf(cpf);
    }

    public String consultarContato(long clienteId) {
        Cliente cli = cli(clienteId);
        return cli.getNome() + "|" + cli.getEmail() + "|" + cli.getCpf();
    }

    public long cadastrarProduto(String nome, double vlrUnit, int estoque, String categoria) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException();
        }
        if (!Double.isFinite(vlrUnit) || vlrUnit <= 0 || estoque < 0) {
            throw new IllegalArgumentException();
        }
        if (!Arrays.asList("LIVRO", "ELETRONICO", "ALIMENTO").contains(categoria)) {
            throw new IllegalArgumentException();
        }
        Produto prod = new Produto();
        prod.setId(seqProd++);
        prod.setNome(nome.trim());
        prod.setVlrUnit(arred(vlrUnit));
        prod.setEstoque(estoque);
        prod.setCategoria(categoria);
        prod.setAtivo(true);
        prods.put(prod.getId(), prod);
        return prod.getId();
    }

    public void alterarPreco(long produtoId, double valor) {
        Produto prod = prod(produtoId);
        if (!Double.isFinite(valor) || valor <= 0) {
            throw new IllegalArgumentException();
        }
        prod.setVlrUnit(arred(valor));
    }

    public void reporEstoque(long produtoId, int qtd) {
        Produto prod = prod(produtoId);
        if (qtd <= 0 || (long) prod.getEstoque() + qtd > Integer.MAX_VALUE) {
            throw new IllegalArgumentException();
        }
        prod.setEstoque(prod.getEstoque() + qtd);
    }

    public int consultarEstoque(long produtoId) {
        return prod(produtoId).getEstoque();
    }

    public void desativarProduto(long produtoId) {
        prod(produtoId).setAtivo(false);
    }

    public long abrirPedido(long clienteId) {
        Cliente cli = cli(clienteId);
        Pedido ped = new Pedido();
        ped.setId(seqPed++);
        ped.setCliente(cli);
        ped.setItens(new ArrayList());
        ped.setStatus("ABERTO");
        ped.setCupom("");
        peds.put(ped.getId(), ped);
        return ped.getId();
    }

    public void adicionarItem(long pedidoId, long produtoId, int qtd) {
        Pedido ped = ped(pedidoId);
        aberto(ped);
        Produto prod = prod(produtoId);
        if (qtd <= 0 || qtd > 100) {
            throw new IllegalArgumentException();
        }
        if (!prod.getAtivo()) {
            throw new IllegalStateException();
        }
        for (Object obj : ped.getItens()) {
            Item item = (Item) obj;
            if (item.getProdutoId() == produtoId) {
                if (item.getQtd() + qtd > 100) {
                    throw new IllegalArgumentException();
                }
                item.setQtd(item.getQtd() + qtd);
                return;
            }
        }
        Item item = new Item();
        item.setProdutoId(produtoId);
        item.setNome(prod.getNome());
        item.setQtd(qtd);
        item.setVlrUnit(prod.getVlrUnit());
        ped.getItens().add(item);
    }

    public void removerItem(long pedidoId, long produtoId) {
        Pedido ped = ped(pedidoId);
        aberto(ped);
        Iterator it = ped.getItens().iterator();
        while (it.hasNext()) {
            Item item = (Item) it.next();
            if (item.getProdutoId() == produtoId) {
                it.remove();
                return;
            }
        }
        throw new NoSuchElementException();
    }

    public void aplicarCupom(long pedidoId, String cupom) {
        Pedido ped = ped(pedidoId);
        aberto(ped);
        if (!Arrays.asList("", "DEZ", "VINTE", "FRETEGRATIS").contains(cupom)) {
            throw new IllegalArgumentException();
        }
        ped.setCupom(cupom);
    }

    public String consultarItens(long pedidoId) {
        Pedido ped = ped(pedidoId);
        String txt = "";
        for (Object obj : ped.getItens()) {
            Item item = (Item) obj;
            if (!txt.isEmpty()) {
                txt += ";";
            }
            txt += String.format(Locale.US, "%d|%s|%d|%.2f", item.getProdutoId(), item.getNome(), item.getQtd(), item.getVlrUnit());
        }
        return txt;
    }

    public double calcularSubtotal(long pedidoId) {
        Pedido ped = ped(pedidoId);
        double vlr = 0;
        for (Object obj : ped.getItens()) {
            Item item = (Item) obj;
            vlr += item.getVlrUnit() * item.getQtd();
        }
        return arred(vlr);
    }

    public String simularPedido(long pedidoId) {
        Pedido ped = ped(pedidoId);
        if (!ped.getStatus().equals("ABERTO")) {
            return resumo(ped);
        }
        double sub = calcularSubtotal(pedidoId);
        double desc = 0;
        if (ped.getCliente().getVip()) {
            desc = arred(sub * 0.05);
        }
        double base = arred(sub - desc);
        if (ped.getCupom().equals("DEZ")) {
            desc += arred(base * 0.10);
        } else if (ped.getCupom().equals("VINTE")) {
            if (sub >= 200) {
                desc += 20;
            }
        }
        double frete;
        String reg = ped.getCliente().getEndereco().getCidade().getEstado().getRegiao();
        if (reg.equals("SUDESTE")) {
            frete = 10;
        } else if (reg.equals("SUL")) {
            frete = 15;
        } else if (reg.equals("CENTRO-OESTE")) {
            frete = 20;
        } else if (reg.equals("NORDESTE")) {
            frete = 25;
        } else {
            frete = 30;
        }
        if (sub >= 300 || ped.getCupom().equals("FRETEGRATIS")) {
            frete = 0;
        }
        ped.setDesconto(arred(desc));
        ped.setFrete(frete);
        ped.setTotal(arred(sub - desc + frete));
        return resumo(ped);
    }

    public String finalizarPedido(long pedidoId) {
        Pedido ped = ped(pedidoId);
        aberto(ped);
        if (ped.getItens().isEmpty()) {
            throw new IllegalStateException();
        }
        for (Object obj : ped.getItens()) {
            Item item = (Item) obj;
            Produto prod = prod(item.getProdutoId());
            if (prod.getAtivo()) {
                if (prod.getEstoque() < item.getQtd()) {
                    throw new IllegalStateException();
                }
            } else {
                throw new IllegalStateException();
            }
        }
        simularPedido(pedidoId);
        for (Object obj : ped.getItens()) {
            Item item = (Item) obj;
            Produto prod = prod(item.getProdutoId());
            prod.setEstoque(prod.getEstoque() - item.getQtd());
        }
        ped.setStatus("FINALIZADO");
        return resumo(ped);
    }

    public void cancelarPedido(long pedidoId) {
        Pedido ped = ped(pedidoId);
        if (ped.getStatus().equals("CANCELADO")) {
            throw new IllegalStateException();
        }
        if (ped.getStatus().equals("FINALIZADO")) {
            for (Object obj : ped.getItens()) {
                Item item = (Item) obj;
                Produto prod = prod(item.getProdutoId());
                if ((long) prod.getEstoque() + item.getQtd() > Integer.MAX_VALUE) {
                    throw new IllegalStateException();
                }
            }
            for (Object obj : ped.getItens()) {
                Item item = (Item) obj;
                Produto prod = prod(item.getProdutoId());
                prod.setEstoque(prod.getEstoque() + item.getQtd());
            }
        }
        ped.setStatus("CANCELADO");
    }

    public String consultarStatus(long pedidoId) {
        return ped(pedidoId).getStatus();
    }

    public String relatorioCliente(long clienteId) {
        cli(clienteId);
        String txt = "";
        for (Object obj : peds.values()) {
            Pedido ped = (Pedido) obj;
            if (ped.getCliente().getId() == clienteId) {
                if (ped.getStatus().equals("FINALIZADO")) {
                    for (Object objItem : ped.getItens()) {
                        Item item = (Item) objItem;
                        if (item.getQtd() >= 2) {
                            txt += String.format(Locale.US, "%d:%s:%d;", ped.getId(), item.getNome(), item.getQtd());
                        }
                    }
                }
            }
        }
        return txt;
    }

    private String resumo(Pedido ped) {
        return String.format(Locale.US, "%s|subtotal=%.2f|desconto=%.2f|frete=%.2f|total=%.2f",
                ped.getStatus(), calcularSubtotal(ped.getId()), ped.getDesconto(), ped.getFrete(), ped.getTotal());
    }

    private Cliente cli(long id) {
        Cliente cli = (Cliente) clis.get(id);
        if (cli == null) {
            throw new NoSuchElementException();
        }
        return cli;
    }

    private Produto prod(long id) {
        Produto prod = (Produto) prods.get(id);
        if (prod == null) {
            throw new NoSuchElementException();
        }
        return prod;
    }

    private Pedido ped(long id) {
        Pedido ped = (Pedido) peds.get(id);
        if (ped == null) {
            throw new NoSuchElementException();
        }
        return ped;
    }

    private void aberto(Pedido ped) {
        if (!ped.getStatus().equals("ABERTO")) {
            throw new IllegalStateException();
        }
    }

    private double arred(double vlr) {
        return BigDecimal.valueOf(vlr).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private boolean vldCpf(String cpf) {
        if (cpf == null || !cpf.matches("[0-9]{11}") || cpf.chars().distinct().count() == 1) {
            return false;
        }
        for (int pos = 9; pos < 11; pos++) {
            int soma = 0;
            for (int i = 0; i < pos; i++) {
                soma += (cpf.charAt(i) - '0') * (pos + 1 - i);
            }
            int dig = (soma * 10) % 11;
            if (dig == 10) {
                dig = 0;
            }
            if (dig != cpf.charAt(pos) - '0') {
                return false;
            }
        }
        return true;
    }
}
