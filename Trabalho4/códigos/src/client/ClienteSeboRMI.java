package client;

import rmi.SeboDispatcherRemote;
import util.JsonHelper;

import java.nio.charset.StandardCharsets;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import pubsub.PubSubSubscriber;
import java.util.Map;
import java.util.Scanner;

public class ClienteSeboRMI {
    private static final String OBJ_REF = "SeboService";

    private static String campoJson(String json, String campo) {
        String chave = "\"" + campo + "\":\"";
        int inicio = json.indexOf(chave);

        if (inicio < 0) {
            return "";
        }

        inicio += chave.length();

        int fim = json.indexOf('"', inicio);

        return fim < 0 ? "" : json.substring(inicio, fim);
    }

    private static String payloadComToken(
            String token,
            String campos
    ) {
        return "{\"token\":\"" + JsonHelper.escapar(token) + "\""
                + (campos == null || campos.isBlank()
                ? ""
                : "," + campos)
                + "}";
    }

    private static void mostrarResposta(byte[] resposta) {
        System.out.println(
                "Resposta: "
                        + new String(resposta, StandardCharsets.UTF_8)
        );
    }

    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {

            String hostServidor = System.getenv()
                    .getOrDefault("SEBO_SERVER_HOST", "localhost");

            String hostCliente = System.getenv("SEBO_CLIENT_HOST");

            if (hostCliente != null && !hostCliente.isBlank()) {
                System.setProperty(
                        "java.rmi.server.hostname",
                        hostCliente
                );
            }

            Registry registry =
                    LocateRegistry.getRegistry(hostServidor, 1099);

            SeboDispatcherRemote stub =
                    (SeboDispatcherRemote) registry.lookup(
                            "SeboDispatcher"
                    );

            ClienteProxyRR proxy = new ClienteProxyRR(stub);

            System.out.println(
                    "=== LOGIN NO SEBO ==="
            );

            System.out.print("Usuário (cliente | admin): ");
            String usuario = scanner.nextLine()
                    .trim()
                    .toLowerCase();

            System.out.print("Senha (use 1234): ");
            String senha = scanner.nextLine();

            String authPayload = String.format(
                    "{\"usuario\":\"%s\",\"senha\":\"%s\"}",
                    JsonHelper.escapar(usuario),
                    JsonHelper.escapar(senha)
            );

            byte[] authRespBytes = proxy.doOperation(
                    OBJ_REF,
                    "autenticar",
                    authPayload.getBytes(StandardCharsets.UTF_8)
            );

            String authRespJson =
                    new String(authRespBytes, StandardCharsets.UTF_8);

            Map<String, String> authMap =
                    JsonHelper.parseObjetoSimples(authRespJson);

            if (!"true".equals(authMap.get("sucesso"))) {
                System.out.println(
                        "Falha no login: " + authMap.get("mensagem")
                );
                return;
            }

            String token = campoJson(authRespJson, "token");
            String perfil = campoJson(authRespJson, "perfil");

            if (token.isBlank() || perfil.isBlank()) {
                System.out.println(
                        "Nao foi possível iniciar a sessao. Tente novamente."
                );
                return;
            }

            System.out.println(authMap.get("mensagem"));

            String brokerHost = System.getenv().getOrDefault("SEBO_BROKER_HOST", "localhost");
            int brokerPort = Integer.parseInt(System.getenv().getOrDefault("SEBO_BROKER_PORT", "5050"));
            PubSubSubscriber assinante = null;
            Thread threadAssinante = null;
            if ("CLIENTE".equals(perfil)) {
                String subscriberId = System.getenv().getOrDefault("SEBO_CLIENT_ID", usuario);
                assinante = new PubSubSubscriber(brokerHost, brokerPort, subscriberId, "avisos-gerais");
                threadAssinante = new Thread(assinante, "assinante-pubsub-" + usuario);
                threadAssinante.setDaemon(true);
                threadAssinante.start();
                System.out.println("[Pub-Sub] Inscrito no tópico avisos-gerais. Voce recebera eventos publicados pelo Broker.");
            }

            boolean executando = true;

            while (executando) {
                System.out.println(
                        "\n--- MENU DO SEBO (RMI + Publish-Subscribe) ---"
                );

                System.out.println("1. Listar acervo");

                if ("ADMIN".equals(perfil)) {
                    System.out.println("2. Cadastrar novo produto");
                }

                System.out.println("3. Comprar produto");
                System.out.println("4. Avaliar troca de produto");

                if ("ADMIN".equals(perfil)) {
                    System.out.println("5. Enviar aviso ao Mural");
                }

                System.out.println("0. Sair");
                System.out.print("Escolha uma opçao: ");

                String opcao = scanner.nextLine();

                switch (opcao) {
                    case "1": {
                        String json = payloadComToken(token, "");

                        mostrarResposta(proxy.doOperation(
                                OBJ_REF,
                                "listarAcervo",
                                json.getBytes(StandardCharsets.UTF_8)
                        ));

                        break;
                    }

                    case "2": {
                        if (!"ADMIN".equals(perfil)) {
                            System.out.println("Acesso negado.");
                            break;
                        }

                        System.out.print(
                                "Tipo (LIVRO, APOSTILA, CD, EBOOK): "
                        );
                        String tipo = scanner.nextLine().toUpperCase();

                        System.out.print("Título: ");
                        String titulo = scanner.nextLine();

                        System.out.print("Preço (ex: 35.50): ");
                        String preco = scanner.nextLine()
                                .replace(",", ".");

                        System.out.print(
                                "Estado (Novo, Seminovo, Usado): "
                        );
                        String estado = scanner.nextLine();

                        System.out.print(
                                "Info extra (Autor / Disciplina / Artista / Formato): "
                        );
                        String extra = scanner.nextLine();

                        String campos = String.format(
                                "\"tipo\":\"%s\",\"titulo\":\"%s\","
                                        + "\"preco\":%s,\"estado\":\"%s\","
                                        + "\"extra\":\"%s\"",
                                JsonHelper.escapar(tipo),
                                JsonHelper.escapar(titulo),
                                preco,
                                JsonHelper.escapar(estado),
                                JsonHelper.escapar(extra)
                        );

                        String json = payloadComToken(token, campos);

                        mostrarResposta(proxy.doOperation(
                                OBJ_REF,
                                "cadastrarProduto",
                                json.getBytes(StandardCharsets.UTF_8)
                        ));

                        break;
                    }

                    case "3": {
                        System.out.print(
                                "ID do produto para compra: "
                        );
                        String id = scanner.nextLine();

                        String json = payloadComToken(
                                token,
                                "\"id\":" + id
                        );

                        mostrarResposta(proxy.doOperation(
                                OBJ_REF,
                                "realizarVenda",
                                json.getBytes(StandardCharsets.UTF_8)
                        ));

                        break;
                    }

                    case "4": {
                        System.out.print(
                                "ID do produto para avaliar troca: "
                        );
                        String id = scanner.nextLine();

                        String json = payloadComToken(
                                token,
                                "\"id\":" + id
                        );

                        mostrarResposta(proxy.doOperation(
                                OBJ_REF,
                                "avaliarTroca",
                                json.getBytes(StandardCharsets.UTF_8)
                        ));

                        break;
                    }

                    case "5": {
                        if (!"ADMIN".equals(perfil)) {
                            System.out.println("Acesso negado.");
                            break;
                        }

                        System.out.print(
                                "Tipo [1-NOTIFICACAO | 2-ALERTA | 3-ATUALIZACAO]: "
                        );

                        String tOp = scanner.nextLine();

                        String tipoAviso = "2".equals(tOp)
                                ? "ALERTA"
                                : ("3".equals(tOp)
                                ? "ATUALIZACAO"
                                : "NOTIFICACAO");

                        System.out.print("Mensagem: ");
                        String msg = scanner.nextLine();

                        String campos = String.format(
                                "\"tipo\":\"%s\",\"mensagem\":\"%s\","
                                        + "\"autor\":\"%s\"",
                                tipoAviso,
                                JsonHelper.escapar(msg),
                                JsonHelper.escapar(usuario)
                        );

                        String json = payloadComToken(token, campos);

                        mostrarResposta(proxy.doOperation(
                                OBJ_REF,
                                "publicarNotificacao",
                                json.getBytes(StandardCharsets.UTF_8)
                        ));

                        break;
                    }

                    case "0": {
                        if (assinante != null) assinante.stop();

                        executando = false;

                        System.out.println("Saindo do sistema...");
                        break;
                    }

                    default:
                        System.out.println("Opçao inválida.");
                }
            }

        } catch (Exception e) {
            System.err.println(
                    "Erro no cliente: " + e.getMessage()
            );
            e.printStackTrace();
        }
    }
}