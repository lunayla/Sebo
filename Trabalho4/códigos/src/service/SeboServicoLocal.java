package service;

import model.*;
import util.JsonHelper;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Objeto local no servidor que processa regras de negócio a partir
 * de parâmetros recebidos POR VALOR (via JSON).
 */
public class SeboServicoLocal {
    private Loja loja;
    private final java.util.concurrent.atomic.AtomicInteger proximoId =
            new java.util.concurrent.atomic.AtomicInteger(4);

    private final Map<String, String> sessoes = new ConcurrentHashMap<>();

    public SeboServicoLocal(Loja loja) {
        this.loja = loja;

        loja.adicionarProduto(new Livro(1, "Sistemas Distribuídos - Coulouris",
                85.00, "Seminovo", "George Coulouris", "978-8582600535"));
        loja.adicionarProduto(new Apostila(2, "RMI e Protocolo Request-Reply",
                20.00, "Novo", "Sistemas Distribuídos"));
        loja.adicionarProduto(new CD(3, "Acústico MTV", 30.00,
                "Seminovo", "Legião Urbana"));
        loja.adicionarProduto(new Ebook(4, "Clean Architecture", 45.00, "PDF"));
    }

    public byte[] autenticar(byte[] argsJsonBytes) {
        String json = new String(argsJsonBytes, StandardCharsets.UTF_8);
        Map<String, String> params = JsonHelper.parseObjetoSimples(json);

        String usuario = params.getOrDefault("usuario", "").trim().toLowerCase();
        String senha = params.getOrDefault("senha", "");

        if (!"1234".equals(senha)
                || !("cliente".equals(usuario) || "admin".equals(usuario))) {

            return JsonHelper.respostaJson(false,
                    "Credenciais invalidas. Use usuario cliente ou admin e senha 1234.",
                    null).getBytes(StandardCharsets.UTF_8);
        }

        String perfil = "admin".equals(usuario) ? "ADMIN" : "CLIENTE";
        String token = UUID.randomUUID().toString();

        sessoes.put(token, perfil);

        String dados = String.format(
                "{\"usuario\":\"%s\",\"perfil\":\"%s\",\"token\":\"%s\"}",
                JsonHelper.escapar(usuario), perfil, token
        );

        return JsonHelper.respostaJson(
                true,
                "Autenticado com sucesso no Sebo! Perfil: " + perfil,
                dados
        ).getBytes(StandardCharsets.UTF_8);
    }

    public String obterPerfil(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }

        return sessoes.get(token);
    }

    public byte[] listarAcervo() {
        String listaJson = JsonHelper.listaProdutosParaJson(loja.getAcervo());

        String resp = JsonHelper.respostaJson(
                true,
                "Acervo recuperado com sucesso.",
                listaJson
        );

        return resp.getBytes(StandardCharsets.UTF_8);
    }

    public byte[] cadastrarProduto(byte[] argsJsonBytes) {
        String json = new String(argsJsonBytes, StandardCharsets.UTF_8);
        Map<String, String> params = JsonHelper.parseObjetoSimples(json);

        String tipo = params.getOrDefault("tipo", "LIVRO").toUpperCase();
        String titulo = params.getOrDefault("titulo", "Sem Título");
        double preco = Double.parseDouble(params.getOrDefault("preco", "10.0"));
        String estado = params.getOrDefault("estado", "Seminovo");
        String extra = params.getOrDefault("extra", "Geral");

        int id = proximoId.incrementAndGet();
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
            case "LIVRO":
                novo = new Livro(id, titulo, preco, estado, extra, "978-000000");
                break;
            default:
                return JsonHelper.respostaJson(
                        false, "Tipo de produto invalido: " + tipo, null
                ).getBytes(StandardCharsets.UTF_8);
        }

        loja.adicionarProduto(novo);

        return JsonHelper.respostaJson(
                true, "Produto cadastrado: " + novo.toString(), null
        ).getBytes(StandardCharsets.UTF_8);
    }

    public byte[] realizarVenda(byte[] argsJsonBytes) {
        String json = new String(argsJsonBytes, StandardCharsets.UTF_8);
        Map<String, String> params = JsonHelper.parseObjetoSimples(json);
        int id = Integer.parseInt(params.getOrDefault("id", "-1"));

        Produto encontrado = loja.getAcervo().stream()
                .filter(p -> p.getId() == id)
                .findFirst()
                .orElse(null);

        if (encontrado == null) {
            return JsonHelper.respostaJson(
                    false, "Produto ID " + id + " nao encontrado.", null
            ).getBytes(StandardCharsets.UTF_8);
        }

        loja.removerProduto(id);

        String msg = String.format(
                "Venda concluída: '%s' por R$ %.2f",
                encontrado.getTitulo(), encontrado.getPreco()
        );

        return JsonHelper.respostaJson(true, msg, null)
                .getBytes(StandardCharsets.UTF_8);
    }

    public byte[] avaliarTroca(byte[] argsJsonBytes) {
        String json = new String(argsJsonBytes, StandardCharsets.UTF_8);
        Map<String, String> params = JsonHelper.parseObjetoSimples(json);
        int id = Integer.parseInt(params.getOrDefault("id", "-1"));

        Produto encontrado = loja.getAcervo().stream()
                .filter(p -> p.getId() == id)
                .findFirst()
                .orElse(null);

        if (encontrado == null) {
            return JsonHelper.respostaJson(
                    false, "Produto ID " + id + " nao encontrado.", null
            ).getBytes(StandardCharsets.UTF_8);
        }

        if (encontrado instanceof Trocavel) {
            Trocavel t = (Trocavel) encontrado;

            if (t.podeTrocar()) {
                String msg = String.format(
                        "Troca APROVADA para '%s'! Credito: R$ %.2f",
                        encontrado.getTitulo(),
                        t.calcularValorTroca()
                );

                return JsonHelper.respostaJson(true, msg, null)
                        .getBytes(StandardCharsets.UTF_8);
            }

            return JsonHelper.respostaJson(
                    false, "Troca RECUSADA: estado de conservaçao inadequado.", null
            ).getBytes(StandardCharsets.UTF_8);
        }

        return JsonHelper.respostaJson(
                false,
                "Troca RECUSADA: " + encontrado.getTipo() + " nao implementa Trocavel.",
                null
        ).getBytes(StandardCharsets.UTF_8);
    }

    public Notificacao criarNotificacao(byte[] argsJsonBytes) {
        String json = new String(argsJsonBytes, StandardCharsets.UTF_8);
        Map<String, String> params = JsonHelper.parseObjetoSimples(json);

        String tipo = params.getOrDefault("tipo", "NOTIFICACAO");
        String mensagem = params.getOrDefault("mensagem", "");
        String autor = params.getOrDefault("autor", "Anonimo");

        Notificacao notif = new Notificacao(
                tipo,
                mensagem,
                System.currentTimeMillis(),
                new ClienteUsuario(autor, "ADMIN")
        );

        loja.registrarNotificacao(notif);
        return notif;
    }
}