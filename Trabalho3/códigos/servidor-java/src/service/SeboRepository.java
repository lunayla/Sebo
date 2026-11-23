package service;

import model.*;
import java.util.concurrent.atomic.AtomicInteger;

public class SeboRepository {
    private final Loja loja;
    private final AtomicInteger seqProduto = new AtomicInteger(4);
    private final AtomicInteger seqNotificacao = new AtomicInteger(1);

    public SeboRepository() {
        this.loja = new Loja("Sebo Universitário UFC Quixadá - Web Service");

        // Carga inicial de produtos
        loja.adicionarProduto(new Livro(1, "Sistemas Distribuídos - Coulouris", 85.00, "Seminovo", "George Coulouris", "978-8582600535"));
        loja.adicionarProduto(new Apostila(2, "Web Services e APIs REST", 20.00, "Novo", "Sistemas Distribuídos"));
        loja.adicionarProduto(new CD(3, "Acústico MTV", 30.00, "Seminovo", "Legião Urbana"));
        loja.adicionarProduto(new Ebook(4, "Designing Data-Intensive Applications", 55.00, "EPUB"));

        // Mensagem inicial de boas-vindas no mural
        loja.adicionarNotificacao(new Notificacao(
            1, "NOTIFICACAO", "Bem-vindo ao Web Service REST do Sebo!",
            System.currentTimeMillis(), new ClienteUsuario("Sistema", "ADMIN")
        ));
    }

    public Loja getLoja() { return loja; }
    public int proximoIdProduto() { return seqProduto.incrementAndGet(); }
    public int proximoIdNotificacao() { return seqNotificacao.incrementAndGet(); }
}
