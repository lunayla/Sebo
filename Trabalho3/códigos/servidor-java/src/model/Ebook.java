package model;

public class Ebook extends Produto {
    private String formato;

    public Ebook(int id, String titulo, double preco, String formato) {
        super(id, titulo, preco, "Digital");
        this.formato = formato;
    }

    @Override
    public String getTipo() { return "EBOOK"; }

    public String getFormato() { return formato; }
}
