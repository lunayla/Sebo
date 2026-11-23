package model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Loja implements Serializable {
    private static final long serialVersionUID = 1L;
    private String nome;
    private List<Produto> acervo;

    public Loja(String nome) {
        this.nome = nome;
        this.acervo = new ArrayList<>();
    }

    public void adicionarProduto(Produto p) {
        acervo.add(p);
    }

    public boolean removerProduto(int id) {
        return acervo.removeIf(p -> p.getId() == id);
    }

    public List<Produto> getAcervo() {
        return acervo;
    }

    public String getNome() {
        return nome;
    }
}
