package util;

import com.sun.net.httpserver.HttpExchange;
import model.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JsonUtil {

    public static String escapar(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }

    public static Map<String, String> parseJsonSimples(String json) {
        Map<String, String> map = new HashMap<>();
        if (json == null || json.isBlank()) return map;
        Pattern p = Pattern.compile("\"([^\"]+)\"\\s*:\\s*(\"([^\"]*)\"|([0-9.-]+|true|false))");
        Matcher m = p.matcher(json);
        while (m.find()) {
            String chave = m.group(1);
            String valor = m.group(3) != null ? m.group(3) : m.group(4);
            map.put(chave, valor);
        }
        return map;
    }

    public static String lerCorpoRequisicao(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    public static void enviarRespostaJson(HttpExchange exchange, int statusCode, String jsonBody) throws IOException {
        byte[] bytes = jsonBody.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    public static String produtosParaJson(List<Produto> produtos) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < produtos.size(); i++) {
            Produto p = produtos.get(i);
            String extra = "";
            if (p instanceof Livro) extra = ((Livro) p).getAutor();
            else if (p instanceof Apostila) extra = ((Apostila) p).getDisciplina();
            else if (p instanceof CD) extra = ((CD) p).getArtista();
            else if (p instanceof Ebook) extra = ((Ebook) p).getFormato();

            sb.append(String.format(Locale.US,
                "{\"id\": %d, \"tipo\": \"%s\", \"titulo\": \"%s\", \"preco\": %.2f, \"estado\": \"%s\", \"extra\": \"%s\", \"trocavel\": %b}",
                p.getId(), escapar(p.getTipo()), escapar(p.getTitulo()), p.getPreco(),
                escapar(p.getEstadoConservacao()), escapar(extra), (p instanceof Trocavel)
            ));
            if (i < produtos.size() - 1) sb.append(", ");
        }
        sb.append("]");
        return sb.toString();
    }

    public static String notificacoesParaJson(List<Notificacao> lista) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < lista.size(); i++) {
            Notificacao n = lista.get(i);
            sb.append(String.format(
                "{\"id\": %d, \"tipo\": \"%s\", \"mensagem\": \"%s\", \"autor\": \"%s\", \"timestamp\": %d}",
                n.getId(), escapar(n.getTipo()), escapar(n.getMensagem()),
                escapar(n.getRemetente() != null ? n.getRemetente().getNomeUsuario() : "Sistema"),
                n.getTimestamp()
            ));
            if (i < lista.size() - 1) sb.append(", ");
        }
        sb.append("]");
        return sb.toString();
    }
}
