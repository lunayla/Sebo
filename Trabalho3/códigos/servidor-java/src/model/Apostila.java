package model;

public class Apostila extends Produto implements Trocavel {
    private String disciplina;

    public Apostila(int id, String titulo, double preco, String estadoConservacao, String disciplina) {
        super(id, titulo, preco, estadoConservacao);
        this.disciplina = disciplina;
    }

    @Override
    public String getTipo() { return "APOSTILA"; }

    @Override
    public boolean podeTrocar() { return true; }

    @Override
    public double calcularValorTroca() { return getPreco() * 0.40; }

    public String getDisciplina() { return disciplina; }
}
