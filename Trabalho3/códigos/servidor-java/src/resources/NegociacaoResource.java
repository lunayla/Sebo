package resources;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import model.Produto;
import model.Trocavel;
import service.SeboRepository;
import util.JsonUtil;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

public class NegociacaoResource implements HttpHandler {
    private final SeboRepository repo;

    public NegociacaoResource(SeboRepository repo) {
        this.repo = repo;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            JsonUtil.enviarRespostaJson(exchange, 405, "{\"sucesso\": false, \"mensagem\": \"Use POST.\"}");
            return;
        }

        String path = exchange.getRequestURI().getPath();
        String body = JsonUtil.lerCorpoRequisicao(exchange);
        Map<String, String> dados = JsonUtil.parseJsonSimples(body);
        int idProduto = Integer.parseInt(dados.getOrDefault("id", "-1"));

        Produto produto = repo.getLoja().buscarProduto(idProduto);
        if (produto == null) {
            JsonUtil.enviarRespostaJson(exchange, 404,
                String.format("{\"sucesso\": false, \"mensagem\": \"Produto ID %d não encontrado.\"}", idProduto));
            return;
        }

        if (path.endsWith("/comprar")) {
            repo.getLoja().removerProduto(idProduto);
            System.out.println("[WS - NegociacaoResource] Compra realizada: ID " + idProduto);
            String msg = String.format(Locale.US, "Compra concluída: '%s' por R$ %.2f.", produto.getTitulo(), produto.getPreco());
            JsonUtil.enviarRespostaJson(exchange, 200,
                String.format("{\"sucesso\": true, \"mensagem\": \"%s\"}", JsonUtil.escapar(msg)));

        } else if (path.endsWith("/trocar")) {
            System.out.println("[WS - NegociacaoResource] Avaliação de troca: ID " + idProduto);
            if (produto instanceof Trocavel) {
                Trocavel t = (Trocavel) produto;
                if (t.podeTrocar()) {
                    String msg = String.format(Locale.US, "Troca APROVADA para '%s'! Crédito gerado: R$ %.2f.",
                            produto.getTitulo(), t.calcularValorTroca());
                    JsonUtil.enviarRespostaJson(exchange, 200,
                        String.format("{\"sucesso\": true, \"mensagem\": \"%s\"}", JsonUtil.escapar(msg)));
                } else {
                    JsonUtil.enviarRespostaJson(exchange, 200,
                        "{\"sucesso\": false, \"mensagem\": \"Troca RECUSADA: estado de conservação não permite troca.\"}");
                }
            } else {
                String msg = "Troca RECUSADA: produtos do tipo " + produto.getTipo() + " (ex: Ebook) não implementam Trocavel.";
                JsonUtil.enviarRespostaJson(exchange, 200,
                    String.format("{\"sucesso\": false, \"mensagem\": \"%s\"}", JsonUtil.escapar(msg)));
            }
        } else {
            JsonUtil.enviarRespostaJson(exchange, 404, "{\"sucesso\": false, \"mensagem\": \"Rota de negociação inválida.\"}");
        }
    }
}
