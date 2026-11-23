package model;

public class CD extends Produto implements Trocavel {
    private String artista;

    public CD(int id, String titulo, double preco, String estadoConservacao, String artista) {
        super(id, titulo, preco, estadoConservacao);
        this.artista = artista;
    }

    @Override
    public String getTipo() { return "CD"; }

    @Override
    public boolean podeTrocar() {
        return getEstadoConservacao().equalsIgnoreCase("Novo") || getEstadoConservacao().equalsIgnoreCase("Seminovo");
    }

    @Override
    public double calcularValorTroca() {
        return podeTrocar() ? getPreco() * 0.50 : 0.0;
    }

    public String getArtista() { return artista; }
}
