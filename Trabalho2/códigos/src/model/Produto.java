package model;

import java.io.Serializable;

public abstract class Produto implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private String titulo;
    private double preco;
    private String estadoConservacao;

    public Produto() {}

    public Produto(int id, String titulo, double preco, String estadoConservacao) {
        this.id = id;
        this.titulo = titulo;
        this.preco = preco;
        this.estadoConservacao = estadoConservacao;
    }

    public abstract String getTipo();

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public double getPreco() { return preco; }
    public void setPreco(double preco) { this.preco = preco; }

    public String getEstadoConservacao() { return estadoConservacao; }
    public void setEstadoConservacao(String estadoConservacao) { this.estadoConservacao = estadoConservacao; }

    @Override
    public String toString() {
        return String.format("[%s] ID: %d | Título: %s | Preço: R$ %.2f | Estado: %s",
                getTipo(), id, titulo, preco, estadoConservacao);
    }
}
