package model;

import java.util.ArrayList;
import java.util.List;

public class Loja {
    private String nome;
    private List<Produto> acervo;          // Agregação 1: Loja "tem-um" conjunto de Produtos
    private List<Notificacao> muralAvisos; // Agregação 2: Loja "tem-um" conjunto de Notificações

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

    public synchronized Produto buscarProduto(int id) {
        return acervo.stream().filter(p -> p.getId() == id).findFirst().orElse(null);
    }

    public synchronized List<Produto> getAcervo() {
        return new ArrayList<>(acervo);
    }

    public synchronized void adicionarNotificacao(Notificacao n) {
        muralAvisos.add(n);
    }

    public synchronized List<Notificacao> getNotificacoesDesde(int ultimoId) {
        List<Notificacao> recentes = new ArrayList<>();
        for (Notificacao n : muralAvisos) {
            if (n.getId() > ultimoId) {
                recentes.add(n);
            }
        }
        return recentes;
    }

    public String getNome() { return nome; }
}
