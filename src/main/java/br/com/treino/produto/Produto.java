package br.com.treino.produto;

public class Produto {
    private long id;
    private String nome;
    private double vlrUnit;
    private int estoque;
    private String categoria;
    private boolean ativo;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public double getVlrUnit() {
        return vlrUnit;
    }

    public void setVlrUnit(double vlrUnit) {
        this.vlrUnit = vlrUnit;
    }

    public int getEstoque() {
        return estoque;
    }

    public void setEstoque(int estoque) {
        this.estoque = estoque;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public boolean getAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }
}
