package model;

public class Livro extends Produto implements Trocavel {
    private static final long serialVersionUID = 1L;
    private String autor;
    private String isbn;

    public Livro(int id, String titulo, double preco, String estadoConservacao, String autor, String isbn) {
        super(id, titulo, preco, estadoConservacao);
        this.autor = autor;
        this.isbn = isbn;
    }

    @Override
    public String getTipo() { return "LIVRO"; }

    @Override
    public boolean podeTrocar() {
        return !getEstadoConservacao().equalsIgnoreCase("Danificado");
    }

    @Override
    public double calcularValorTroca() {
        return podeTrocar() ? getPreco() * 0.60 : 0.0;
    }

    public String getAutor() { return autor; }
    public String getIsbn() { return isbn; }
}
