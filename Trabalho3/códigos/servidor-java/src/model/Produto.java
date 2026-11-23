package model;

public abstract class Produto {
    private int id;
    private String titulo;
    private double preco;
    private String estadoConservacao;

    public Produto(int id, String titulo, double preco, String estadoConservacao) {
        this.id = id;
        this.titulo = titulo;
        this.preco = preco;
        this.estadoConservacao = estadoConservacao;
    }

    public abstract String getTipo();

    public int getId() { return id; }
    public String getTitulo() { return titulo; }
    public double getPreco() { return preco; }
    public String getEstadoConservacao() { return estadoConservacao; }

    @Override
    public String toString() {
        return String.format("[%s] ID: %d | Título: %s | Preço: R$ %.2f | Estado: %s",
                getTipo(), id, titulo, preco, estadoConservacao);
    }
}
