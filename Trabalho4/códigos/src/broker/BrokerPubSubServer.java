
package broker;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Base64;

/**
 * Broker Publish-Subscribe com tópicos,
 * assinaturas duráveis e log persistente.
 */
public class BrokerPubSubServer {

    private static final int PORT = Integer.parseInt(
            System.getenv().getOrDefault("SEBO_BROKER_PORT", "5050")
    );

    private static final Path LOG = Paths.get("dados-pubsub", "broker.log");

    private final Object lock = new Object();

    private final Map<String, Set<String>> subscribersByTopic = new HashMap<>();

    private final Map<String, Event> events = new LinkedHashMap<>();

    private final Map<String, Set<String>> pendingBySubscriber = new HashMap<>();

    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    private final AtomicLong sequence = new AtomicLong(1);

    /**
     * Representa um evento publicado.
     */
    private static class Event {
        final String topic;
        final String payload;

        Event(String topic, String payload) {
            this.topic = topic;
            this.payload = payload;
        }
    }

    /**
     * Representa a conexão ativa de um assinante.
     */
    private static class Session {
        final BufferedWriter out;

        Session(BufferedWriter out) {
            this.out = out;
        }

        synchronized void send(String line) throws IOException {
            out.write(line);
            out.newLine();
            out.flush();
        }
    }

    public BrokerPubSubServer() throws IOException {
        Files.createDirectories(LOG.getParent());
        replay();
    }

    /**
     * Registra uma operação no arquivo de log.
     */
    private void append(String record) throws IOException {
        Files.writeString(
                LOG,
                record + System.lineSeparator(),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
        );
    }

    /**
     * Recupera o estado do Broker a partir do log persistente.
     */
    private void replay() throws IOException {
        if (!Files.exists(LOG)) {
            return;
        }

        for (String line : Files.readAllLines(LOG, StandardCharsets.UTF_8)) {
            String[] p = line.split("\\|", 4);

            try {
                switch (p[0]) {

                    case "SUB":
                        if (p.length >= 3) {
                            String subscriber = dec(p[1]);
                            String topic = dec(p[2]);

                            subscribersByTopic
                                    .computeIfAbsent(topic, k -> new HashSet<>())
                                    .add(subscriber);
                        }
                        break;

                    case "PUB":
                        if (p.length >= 4) {
                            String id = p[1];
                            String topic = dec(p[2]);
                            String payload = dec(p[3]);

                            events.put(id, new Event(topic, payload));

                            Set<String> subscribers =
                                    subscribersByTopic.getOrDefault(
                                            topic,
                                            Collections.emptySet()
                                    );

                            for (String subscriber : subscribers) {
                                pendingBySubscriber
                                        .computeIfAbsent(
                                                subscriber,
                                                k -> new LinkedHashSet<>()
                                        )
                                        .add(id);
                            }

                            sequence.set(
                                    Math.max(
                                            sequence.get(),
                                            Long.parseLong(id) + 1
                                    )
                            );
                        }
                        break;

                    case "ACK":
                        if (p.length >= 3) {
                            String subscriber = dec(p[1]);
                            String id = p[2];

                            pendingBySubscriber
                                    .computeIfAbsent(
                                            subscriber,
                                            k -> new LinkedHashSet<>()
                                    )
                                    .remove(id);
                        }
                        break;
                }

            } catch (Exception e) {
                System.err.println("Registro ignorado no log: " + line);
            }
        }

        System.out.println(
                "[Broker] Log recuperado. Eventos armazenados: "
                        + events.size()
        );
    }

    /**
     * Codifica uma mensagem em Base64.
     */
    private static String enc(String value) {
        return Base64.getEncoder().encodeToString(
                value.getBytes(StandardCharsets.UTF_8)
        );
    }

    /**
     * Decodifica uma mensagem em Base64.
     */
    private static String dec(String value) {
        return new String(
                Base64.getDecoder().decode(value),
                StandardCharsets.UTF_8
        );
    }

    /**
     * Registra um assinante em determinado tópico.
     */
    private void subscribe(
            String subscriber,
            String topic,
            Session session
    ) throws IOException {

        synchronized (lock) {

            Set<String> subscribers =
                    subscribersByTopic.computeIfAbsent(
                            topic,
                            k -> new HashSet<>()
                    );

            if (subscribers.add(subscriber)) {
                append("SUB|" + enc(subscriber) + "|" + enc(topic));
            }

            sessions.put(subscriber, session);

            System.out.println(
                    "[Broker] SUBSCRIBE: " + subscriber + " -> " + topic
            );

            deliverPending(subscriber, session);
        }
    }

    /**
     * Publica um evento em determinado tópico.
     */
    private void publish(String topic, String payload) throws IOException {

        synchronized (lock) {

            String id = String.valueOf(sequence.getAndIncrement());

            append(
                    "PUB|" + id + "|" + enc(topic) + "|" + enc(payload)
            );

            events.put(id, new Event(topic, payload));

            Set<String> subscribers =
                    subscribersByTopic.getOrDefault(
                            topic,
                            Collections.emptySet()
                    );

            for (String subscriber : subscribers) {

                pendingBySubscriber
                        .computeIfAbsent(
                                subscriber,
                                k -> new LinkedHashSet<>()
                        )
                        .add(id);

                Session session = sessions.get(subscriber);

                if (session != null) {
                    try {
                        deliverOne(id, session);

                    } catch (IOException e) {
                        removeSessionIfCurrent(subscriber, session);

                        System.out.println(
                                "[Broker] Assinante desconectou durante "
                                        + "a entrega; evento mantido pendente: "
                                        + subscriber
                        );
                    }
                }
            }

            System.out.println(
                    "[Broker] PUBLISH evento=" + id
                            + " tópico=" + topic
                            + " destinatários=" + subscribers.size()
            );
        }
    }

    /**
     * Tenta entregar todos os eventos pendentes de um assinante.
     */
    private void deliverPending(String subscriber, Session session) {

        Set<String> pending = pendingBySubscriber.getOrDefault(
                subscriber,
                Collections.emptySet()
        );

        for (String id : new ArrayList<>(pending)) {

            try {
                deliverOne(id, session);

            } catch (IOException e) {
                removeSessionIfCurrent(subscriber, session);

                System.out.println(
                        "[Broker] Assinante desconectou; evento continuará "
                                + "pendente: " + subscriber
                );

                break;
            }
        }
    }

    /**
     * Envia um evento para uma sessão.
     */
    private void deliverOne(String id, Session session) throws IOException {

        Event event = events.get(id);

        if (event == null) {
            return;
        }

        session.send(
                "EVENT|" + id
                        + "|" + enc(event.topic)
                        + "|" + enc(event.payload)
        );
    }

    /**
     * Registra a confirmação de recebimento de um evento.
     */
    private void ack(String subscriber, String id) throws IOException {

        synchronized (lock) {

            Set<String> pending = pendingBySubscriber.get(subscriber);

            if (pending != null && pending.remove(id)) {
                append("ACK|" + enc(subscriber) + "|" + id);
            }
        }
    }

    /**
     * Remove uma sessão somente se ela ainda for a conexão atual.
     */
    private void removeSessionIfCurrent(
            String subscriber,
            Session session
    ) {

        if (sessions.get(subscriber) == session) {
            sessions.remove(subscriber);
        }
    }

    /**
     * Trata a comunicação com um cliente conectado ao Broker.
     */
    private void handle(Socket socket) {

        String subscriber = null;
        Session currentSession = null;

        try (
                socket;
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(
                                socket.getInputStream(),
                                StandardCharsets.UTF_8
                        )
                );
                BufferedWriter out = new BufferedWriter(
                        new OutputStreamWriter(
                                socket.getOutputStream(),
                                StandardCharsets.UTF_8
                        )
                )
        ) {

            String line;

            while ((line = in.readLine()) != null) {

                String[] p = line.split("\\|", 4);

                if (p.length == 0) {
                    continue;
                }

                switch (p[0]) {

                    case "SUB":
                        if (p.length >= 3) {

                            subscriber = dec(p[1]);

                            currentSession = new Session(out);

                            subscribe(
                                    subscriber,
                                    dec(p[2]),
                                    currentSession
                            );

                            currentSession.send("OK|SUBSCRIBED");
                        }
                        break;

                    case "PUB":
                        if (p.length >= 3) {

                            publish(dec(p[1]), dec(p[2]));

                            out.write("OK|PUBLISHED");
                            out.newLine();
                            out.flush();
                        }
                        break;

                    case "ACK":
                        if (p.length >= 2 && subscriber != null) {
                            ack(subscriber, p[1]);
                        }
                        break;

                    default:
                        System.out.println(
                                "[Broker] Comando desconhecido: " + p[0]
                        );
                        break;
                }
            }

        } catch (Exception e) {

            if (subscriber != null) {

                synchronized (lock) {
                    removeSessionIfCurrent(subscriber, currentSession);
                }

                System.out.println(
                        "[Broker] Assinante offline: " + subscriber
                                + " (eventos pendentes preservados)"
                );
            }
        }
    }

    /**
     * Inicia o servidor Broker.
     */
    public void start() throws IOException {

        try (ServerSocket server = new ServerSocket(PORT)) {

            System.out.println("============================================");
            System.out.println(
                    " Broker Publish-Subscribe ativo na porta " + PORT
            );
            System.out.println("============================================");

            while (true) {

                Socket socket = server.accept();

                Thread thread = new Thread(
                        () -> handle(socket)
                );

                thread.setDaemon(true);
                thread.start();
            }
        }
    }

    public static void main(String[] args) throws Exception {
        new BrokerPubSubServer().start();
    }
}
