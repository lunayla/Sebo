package client;

import rmi.SeboDispatcherRemote;
import util.JsonHelper;

import java.nio.charset.StandardCharsets;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;
import java.util.Scanner;

public class ClienteSeboRMI {
    private static final String OBJ_REF = "SeboService";

    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            // 1. Localiza objeto remoto no RMI Registry (Passagem por referência)
            String hostServidor = System.getenv().getOrDefault("SEBO_SERVER_HOST", "localhost");
            String hostCliente = System.getenv("SEBO_CLIENT_HOST");
            if (hostCliente != null && !hostCliente.isBlank()) {
                System.setProperty("java.rmi.server.hostname", hostCliente);
            }
            Registry registry = LocateRegistry.getRegistry(hostServidor, 1099);
            SeboDispatcherRemote stub = (SeboDispatcherRemote) registry.lookup("SeboDispatcher");
            ClienteProxyRR proxy = new ClienteProxyRR(stub);

            // 2. Autenticação via doOperation()
            System.out.println("=== LOGIN NO SEBO DISTRIBUÍDO (RMI) ===");
            System.out.print("Usuário: ");
            String usuario = scanner.nextLine();
            System.out.print("Senha (use 1234): ");
            String senha = scanner.nextLine();

            String authPayload = String.format("{\"usuario\": \"%s\", \"senha\": \"%s\"}",
                    JsonHelper.escapar(usuario), JsonHelper.escapar(senha));
            byte[] authRespBytes = proxy.doOperation(OBJ_REF, "autenticar", authPayload.getBytes(StandardCharsets.UTF_8));
            String authRespJson = new String(authRespBytes, StandardCharsets.UTF_8);
            Map<String, String> authMap = JsonHelper.parseObjetoSimples(authRespJson);

            if (!"true".equals(authMap.get("sucesso"))) {
                System.out.println("Falha no login: " + authMap.get("mensagem"));
                return;
            }
            System.out.println(authMap.get("mensagem"));

            // 3. Registra referência remota de callback para notificações em tempo real
            ClienteCallbackImpl callback = new ClienteCallbackImpl(usuario);
            stub.registrarCallbackCliente(callback);

            // 4. Loop de interação com o usuário
            boolean executando = true;
            while (executando) {
                System.out.println("\n--- MENU DO SEBO (RMI Request-Reply) ---");
                System.out.println("1. Listar acervo");
                System.out.println("2. Cadastrar novo produto (Livro, Apostila, CD ou Ebook)");
                System.out.println("3. Comprar produto");
                System.out.println("4. Avaliar troca de produto");
                System.out.println("5. Enviar aviso ao Mural (NOTIFICACAO / ALERTA / ATUALIZACAO)");
                System.out.println("0. Sair");
                System.out.print("Escolha uma opção: ");

                String opcao = scanner.nextLine();
                switch (opcao) {
                    case "1": {
                        byte[] resp = proxy.doOperation(OBJ_REF, "listarAcervo", new byte[0]);
                        System.out.println("Resposta JSON: " + new String(resp, StandardCharsets.UTF_8));
                        break;
                    }
                    case "2": {
                        System.out.print("Tipo (LIVRO, APOSTILA, CD, EBOOK): ");
                        String tipo = scanner.nextLine().toUpperCase();
                        System.out.print("Título: ");
                        String titulo = scanner.nextLine();
                        System.out.print("Preço (ex: 35.50): ");
                        String preco = scanner.nextLine().replace(",", ".");
                        System.out.print("Estado (Novo, Seminovo, Usado): ");
                        String estado = scanner.nextLine();
                        System.out.print("Info extra (Autor / Disciplina / Artista / Formato): ");
                        String extra = scanner.nextLine();

                        String jsonArgs = String.format(
                            "{\"tipo\": \"%s\", \"titulo\": \"%s\", \"preco\": %s, \"estado\": \"%s\", \"extra\": \"%s\"}",
                            JsonHelper.escapar(tipo), JsonHelper.escapar(titulo), preco,
                            JsonHelper.escapar(estado), JsonHelper.escapar(extra)
                        );
                        byte[] resp = proxy.doOperation(OBJ_REF, "cadastrarProduto", jsonArgs.getBytes(StandardCharsets.UTF_8));
                        System.out.println("Resposta: " + new String(resp, StandardCharsets.UTF_8));
                        break;
                    }
                    case "3": {
                        System.out.print("ID do produto para compra: ");
                        String id = scanner.nextLine();
                        String jsonArgs = String.format("{\"id\": %s}", id);
                        byte[] resp = proxy.doOperation(OBJ_REF, "realizarVenda", jsonArgs.getBytes(StandardCharsets.UTF_8));
                        System.out.println("Resposta: " + new String(resp, StandardCharsets.UTF_8));
                        break;
                    }
                    case "4": {
                        System.out.print("ID do produto para avaliar troca: ");
                        String id = scanner.nextLine();
                        String jsonArgs = String.format("{\"id\": %s}", id);
                        byte[] resp = proxy.doOperation(OBJ_REF, "avaliarTroca", jsonArgs.getBytes(StandardCharsets.UTF_8));
                        System.out.println("Resposta: " + new String(resp, StandardCharsets.UTF_8));
                        break;
                    }
                    case "5": {
                        System.out.print("Tipo [1-NOTIFICACAO | 2-ALERTA | 3-ATUALIZACAO]: ");
                        String tOp = scanner.nextLine();
                        String tipoAviso = "2".equals(tOp) ? "ALERTA" : ("3".equals(tOp) ? "ATUALIZACAO" : "NOTIFICACAO");
                        System.out.print("Mensagem: ");
                        String msg = scanner.nextLine();

                        String jsonArgs = String.format(
                            "{\"tipo\": \"%s\", \"mensagem\": \"%s\", \"autor\": \"%s\"}",
                            tipoAviso, JsonHelper.escapar(msg), JsonHelper.escapar(usuario)
                        );
                        byte[] resp = proxy.doOperation(OBJ_REF, "publicarNotificacao", jsonArgs.getBytes(StandardCharsets.UTF_8));
                        System.out.println("Status publicação: " + new String(resp, StandardCharsets.UTF_8));
                        break;
                    }
                    case "0": {
                        stub.removerCallbackCliente(callback);
                        UnicastRemoteObject.unexportObject(callback, true);
                        executando = false;
                        System.out.println("Saindo do sistema...");
                        break;
                    }
                    default:
                        System.out.println("Opção inválida.");
                }
            }
        } catch (Exception e) {
            System.err.println("Erro no cliente RMI: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
