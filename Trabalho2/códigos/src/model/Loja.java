package model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Loja implements Serializable {
    private static final long serialVersionUID = 1L;
    private String nome;
    private List<Produto> acervo;           // Agregação 1: Loja "tem-um" conjunto de Produtos
    private List<Notificacao> muralAvisos;  // Agregação 2: Loja "tem-um" conjunto de Notificações

    public Loja(String nome) {
        this.nome = nome;
        this.acervo = new ArrayList<>();
        this.muralAvisos = new ArrayList<>();
    }

    public synchronized void adicionarProduto(Produto p) {
        acervo.add(p);
    }

    public synchronized boolean removerProduto(int id) {
        return acervo.removeIf(p -> p.getId() == id);
    }

    public synchronized List<Produto> getAcervo() {
        return new ArrayList<>(acervo);
    }

    public synchronized void registrarNotificacao(Notificacao n) {
        muralAvisos.add(n);
    }

    public synchronized List<Notificacao> getMuralAvisos() {
        return new ArrayList<>(muralAvisos);
    }

    public String getNome() {
        return nome;
    }
}
