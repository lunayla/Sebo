package resources;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import model.ClienteUsuario;
import model.Notificacao;
import service.SeboRepository;
import util.JsonUtil;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class MuralResource implements HttpHandler {
    private final SeboRepository repo;

    public MuralResource(SeboRepository repo) {
        this.repo = repo;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String metodo = exchange.getRequestMethod().toUpperCase();

        if (!AuthResource.exigirLogin(exchange)) {
            return;
        }

        if ("GET".equals(metodo)) {
            String query = exchange.getRequestURI().getQuery();
            int desde = 0;

            if (query != null && query.startsWith("desde=")) {
                try {
                    desde = Integer.parseInt(query.substring(6));
                } catch (NumberFormatException ignored) {
                }
            }

            List<Notificacao> recentes =
                repo.getLoja().getNotificacoesDesde(desde);

            String jsonLista = JsonUtil.notificacoesParaJson(recentes);

            String resp = String.format(
                "{\"sucesso\": true, \"notificacoes\": %s}",
                jsonLista
            );

            JsonUtil.enviarRespostaJson(exchange, 200, resp);

        } else if ("POST".equals(metodo)) {

            if (!AuthResource.exigirAdmin(exchange)) {
                return;
            }

            String body = JsonUtil.lerCorpoRequisicao(exchange);
            Map<String, String> dados = JsonUtil.parseJsonSimples(body);

            String tipo = dados.getOrDefault("tipo", "NOTIFICACAO").toUpperCase();
            String mensagem = dados.getOrDefault("mensagem", "");
            String autor = dados.getOrDefault("autor", "Anônimo");

            int id = repo.proximoIdNotificacao();

            Notificacao notif = new Notificacao(
                id,
                tipo,
                mensagem,
                System.currentTimeMillis(),
                new ClienteUsuario(autor, AuthResource.obterPerfil(exchange))
            );

            repo.getLoja().adicionarNotificacao(notif);

            System.out.printf(
                "[WS - MuralResource] Nova publicação [%s] de %s: %s%n",
                tipo, autor, mensagem
            );

            String resp = String.format(
                "{\"sucesso\": true, \"mensagem\": \"Aviso publicado no mural!\", \"id\": %d}",
                id
            );

            JsonUtil.enviarRespostaJson(exchange, 201, resp);

        } else {
            JsonUtil.enviarRespostaJson(exchange, 405,
                "{\"sucesso\": false, \"mensagem\": \"Use GET ou POST.\"}");
        }
    }
}