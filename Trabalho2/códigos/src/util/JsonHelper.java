package util;

import model.*;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Locale;

/** Representação externa de dados em JSON para objetos simples. */
public class JsonHelper {
    public static String escapar(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    /** Lê objetos JSON planos cujos valores são strings, números ou booleanos. */
    public static Map<String, String> parseObjetoSimples(String json) {
        Map<String, String> resultado = new HashMap<>();
        if (json == null || json.isBlank()) return resultado;
        int[] pos = {0};
        pularEspacos(json, pos);
        if (pos[0] >= json.length() || json.charAt(pos[0]++) != '{') return resultado;
        while (pos[0] < json.length()) {
            pularEspacos(json, pos);
            if (pos[0] < json.length() && json.charAt(pos[0]) == '}') break;
            String chave = lerString(json, pos);
            if (chave == null) break;
            pularEspacos(json, pos);
            if (pos[0] >= json.length() || json.charAt(pos[0]++) != ':') break;
            pularEspacos(json, pos);
            String valor;
            if (pos[0] < json.length() && json.charAt(pos[0]) == '"') {
                valor = lerString(json, pos);
            } else {
                int inicio = pos[0];
                while (pos[0] < json.length() && json.charAt(pos[0]) != ',' && json.charAt(pos[0]) != '}') pos[0]++;
                valor = json.substring(inicio, pos[0]).trim();
            }
            if (valor == null) break;
            resultado.put(chave, valor);
            pularEspacos(json, pos);
            if (pos[0] < json.length() && json.charAt(pos[0]) == ',') { pos[0]++; continue; }
            break;
        }
        return resultado;
    }

    private static void pularEspacos(String s, int[] p) {
        while (p[0] < s.length() && Character.isWhitespace(s.charAt(p[0]))) p[0]++;
    }

    private static String lerString(String s, int[] p) {
        if (p[0] >= s.length() || s.charAt(p[0]++) != '"') return null;
        StringBuilder b = new StringBuilder();
        while (p[0] < s.length()) {
            char c = s.charAt(p[0]++);
            if (c == '"') return b.toString();
            if (c == '\\') {
                if (p[0] >= s.length()) return null;
                char e = s.charAt(p[0]++);
                switch (e) {
                    case '"': b.append('"'); break;
                    case '\\': b.append('\\'); break;
                    case '/': b.append('/'); break;
                    case 'n': b.append('\n'); break;
                    case 'r': b.append('\r'); break;
                    case 't': b.append('\t'); break;
                    case 'b': b.append('\b'); break;
                    case 'f': b.append('\f'); break;
                    case 'u':
                        if (p[0] + 4 > s.length()) return null;
                        try { b.append((char) Integer.parseInt(s.substring(p[0], p[0] + 4), 16)); }
                        catch (NumberFormatException ex) { return null; }
                        p[0] += 4;
                        break;
                    default: return null;
                }
            } else b.append(c);
        }
        return null;
    }

    public static String notificacaoParaJson(Notificacao n) {
        return String.format(Locale.US,
            "{\"tipo\":\"%s\",\"mensagem\":\"%s\",\"autor\":\"%s\",\"timestamp\":%d}",
            escapar(n.getTipo()), escapar(n.getMensagem()),
            escapar(n.getRemetente() != null ? n.getRemetente().getNomeUsuario() : "Sistema"), n.getTimestamp());
    }

    public static String produtoParaJson(Produto p) {
        return String.format(Locale.US,
            "{\"id\":%d,\"tipo\":\"%s\",\"titulo\":\"%s\",\"preco\":%.2f,\"estado\":\"%s\"}",
            p.getId(), escapar(p.getTipo()), escapar(p.getTitulo()), p.getPreco(), escapar(p.getEstadoConservacao()));
    }

    public static String listaProdutosParaJson(List<Produto> produtos) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < produtos.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(produtoParaJson(produtos.get(i)));
        }
        return sb.append(']').toString();
    }

    public static String respostaJson(boolean sucesso, String mensagem, String dadosExtrasJson) {
        return String.format(Locale.US, "{\"sucesso\":%b,\"mensagem\":\"%s\",\"dados\":%s}",
            sucesso, escapar(mensagem), (dadosExtrasJson == null || dadosExtrasJson.isBlank()) ? "null" : dadosExtrasJson);
    }
}
