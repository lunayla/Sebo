
package pubsub;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import util.JsonHelper;

/**
 * Assinante durável do sistema Publish-Subscribe.
 * A identidade é lógica (usuário), não endereço de rede.
 */
public class PubSubSubscriber implements Runnable {

    private final String host;
    private final String subscriberId;
    private final String topic;
    private final int port;

    private volatile boolean running = true;

    public PubSubSubscriber(
            String host,
            int port,
            String subscriberId,
            String topic) {

        this.host = host;
        this.port = port;
        this.subscriberId = subscriberId;
        this.topic = topic;
    }

    public void stop() {
        running = false;
    }

    @Override
    public void run() {

        while (running) {

            try (
                Socket s = new Socket(host, port);

                BufferedReader in = new BufferedReader(
                    new InputStreamReader(
                        s.getInputStream(),
                        StandardCharsets.UTF_8
                    )
                );

                BufferedWriter out = new BufferedWriter(
                    new OutputStreamWriter(
                        s.getOutputStream(),
                        StandardCharsets.UTF_8
                    )
                )
            ) {

                out.write(
                    "SUB|" + enc(subscriberId) + "|" + enc(topic) + "\n"
                );
                out.flush();

                String line;

                while (running && (line = in.readLine()) != null) {

                    String[] p = line.split("\\|", 4);

                    if ("OK".equals(p[0])) {
                        continue;
                    }

                    if ("EVENT".equals(p[0]) && p.length == 4) {

                        String id = p[1];
                        String receivedTopic = dec(p[2]);
                        String payload = dec(p[3]);

                        mostrarNotificacao(receivedTopic, payload);

                        // Confirma o recebimento ao Broker.
                        out.write("ACK|" + id + "\n");
                        out.flush();

                        System.out.print("\nEscolha uma opção: ");
                    }
                }

            } catch (Exception e) {

                if (running) {

                    System.out.println(
                        "\n[PubSub] Broker indisponível; " +
                        "tentando reconectar em 3 segundos..."
                    );

                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        }
    }

    private void mostrarNotificacao(String receivedTopic, String payload) {

        Map<String, String> dados =
            JsonHelper.parseObjetoSimples(payload);

        String tipo = dados.getOrDefault("tipo", "NOTIFICACAO");
        String mensagem = dados.getOrDefault("mensagem", payload);
        String autor = dados.getOrDefault("autor", "Anônimo");

        System.out.println("\n");
        System.out.println("========================================");
        System.out.println("           NOVA NOTIFICAÇÃO");
        System.out.println("========================================");
        System.out.println("Tipo:     " + tipo);
        System.out.println("Mensagem: " + mensagem);
        System.out.println("Autor:    " + autor);
        System.out.println("========================================");
    }

    private static String enc(String s) {
        return Base64.getEncoder().encodeToString(
            s.getBytes(StandardCharsets.UTF_8)
        );
    }

    private static String dec(String s) {
        return new String(
            Base64.getDecoder().decode(s),
            StandardCharsets.UTF_8
        );
    }
}
