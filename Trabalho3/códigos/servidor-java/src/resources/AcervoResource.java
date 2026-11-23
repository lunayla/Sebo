package resources;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import model.*;
import service.SeboRepository;
import util.JsonUtil;

import java.io.IOException;
import java.util.Map;

public class AcervoResource implements HttpHandler {
    private final SeboRepository repo;

    public AcervoResource(SeboRepository repo) {
        this.repo = repo;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String metodo = exchange.getRequestMethod().toUpperCase();

        if (!AuthResource.exigirLogin(exchange)) {
            return;
        }

        if ("GET".equals(metodo)) {
            System.out.println("[WS - AcervoResource] GET /api/produtos");

            String listaJson =
                JsonUtil.produtosParaJson(repo.getLoja().getAcervo());

            String resp = String.format(
                "{\"sucesso\": true, \"loja\": \"%s\", \"produtos\": %s}",
                JsonUtil.escapar(repo.getLoja().getNome()),
                listaJson
            );

            JsonUtil.enviarRespostaJson(exchange, 200, resp);

        } else if ("POST".equals(metodo)) {

            if (!AuthResource.exigirAdmin(exchange)) {
                return;
            }

            String body = JsonUtil.lerCorpoRequisicao(exchange);
            Map<String, String> dados = JsonUtil.parseJsonSimples(body);

            String tipo = dados.getOrDefault("tipo", "LIVRO").toUpperCase();
            String titulo = dados.getOrDefault("titulo", "Item sem título");
            double preco = Double.parseDouble(dados.getOrDefault("preco", "10.0"));
            String estado = dados.getOrDefault("estado", "Seminovo");
            String extra = dados.getOrDefault("extra", "Geral");

            int id = repo.proximoIdProduto();
            Produto novo;

            switch (tipo) {
                case "APOSTILA":
                    novo = new Apostila(id, titulo, preco, estado, extra);
                    break;

                case "CD":
                    novo = new CD(id, titulo, preco, estado, extra);
                    break;

                case "EBOOK":
                    novo = new Ebook(id, titulo, preco, extra);
                    break;

                default:
                    novo = new Livro(id, titulo, preco, estado, extra, "978-000000");
                    break;
            }

            repo.getLoja().adicionarProduto(novo);

            System.out.println(
                "[WS - AcervoResource] POST /api/produtos -> Cadastrado ID " + id
            );

            String resp = String.format(
                "{\"sucesso\": true, \"mensagem\": \"Produto cadastrado com sucesso!\", \"id\": %d}",
                id
            );

            JsonUtil.enviarRespostaJson(exchange, 201, resp);

        } else {
            JsonUtil.enviarRespostaJson(exchange, 405,
                "{\"sucesso\": false, \"mensagem\": \"Use GET ou POST.\"}");
        }
    }
}