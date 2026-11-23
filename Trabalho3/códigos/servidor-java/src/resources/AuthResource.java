package resources;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import util.JsonUtil;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AuthResource implements HttpHandler {
    private static final Map<String, String> SESSOES = new ConcurrentHashMap<>();

    public static String obterPerfil(HttpExchange exchange) {
        String header = exchange.getRequestHeaders().getFirst("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            return null;
        }

        return SESSOES.get(header.substring(7).trim());
    }

    public static boolean exigirLogin(HttpExchange exchange) throws IOException {
        if (obterPerfil(exchange) != null) {
            return true;
        }

        JsonUtil.enviarRespostaJson(exchange, 401,
            "{\"sucesso\": false, \"mensagem\": \"Faça login para acessar esta operação.\"}");

        return false;
    }

    public static boolean exigirAdmin(HttpExchange exchange) throws IOException {
        String perfil = obterPerfil(exchange);

        if ("ADMIN".equals(perfil)) {
            return true;
        }

        int status = perfil == null ? 401 : 403;

        String mensagem = perfil == null
            ? "Faça login para acessar esta operação."
            : "Acesso permitido somente ao administrador.";

        JsonUtil.enviarRespostaJson(exchange, status,
            "{\"sucesso\": false, \"mensagem\": \"" + mensagem + "\"}");

        return false;
    }

    
@Override
public void handle(HttpExchange exchange) throws IOException {
    if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
        JsonUtil.enviarRespostaJson(exchange, 405,
            "{\"sucesso\": false, \"mensagem\": \"Use POST.\"}");
        return;
    }

    Map<String, String> dados =
        JsonUtil.parseJsonSimples(JsonUtil.lerCorpoRequisicao(exchange));

    String usuario = dados.getOrDefault("usuario", "").trim().toLowerCase();
    String senha = dados.getOrDefault("senha", "");

    System.out.println("[WS - AuthResource] Tentativa de login: " + usuario);

    String perfil;

    if ("cliente".equals(usuario)) {
        perfil = "CLIENTE";
    } else if ("admin".equals(usuario)) {
        perfil = "ADMIN";
    } else {
        perfil = null;
    }

    if (perfil != null && "1234".equals(senha)) {
        String token = UUID.randomUUID().toString();

        SESSOES.put(token, perfil);

        System.out.println("[WS - AuthResource] Login realizado: " + perfil);

        String resp = String.format(
            "{\"sucesso\": true, \"mensagem\": \"Login realizado com sucesso!\", \"usuario\": \"%s\", \"perfil\": \"%s\", \"token\": \"%s\"}",
            JsonUtil.escapar(usuario), perfil, token
        );

        JsonUtil.enviarRespostaJson(exchange, 200, resp);

    } else {
        System.out.println("[WS - AuthResource] Login recusado: " + usuario);

        JsonUtil.enviarRespostaJson(exchange, 401,
            "{\"sucesso\": false, \"mensagem\": \"Credenciais inválidas. Use cliente/1234 ou admin/1234.\"}");
    }

    }

}