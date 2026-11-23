package multicast;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServidorMulticastSebo {
    private static final String GRUPO_IP = "230.0.0.1";
    private static final int PORTA_MULTICAST = 4446;
    private static final int PORTA_AUTH_TCP = 5003;

    public static void main(String[] args) throws Exception {
        ExecutorService pool = Executors.newCachedThreadPool();

        pool.execute(() -> {
            try (ServerSocket tcpServer = new ServerSocket(PORTA_AUTH_TCP)) {
                System.out.println("[TCP Auth] Servidor de autenticação ativo na porta " + PORTA_AUTH_TCP);
                while (true) {
                    Socket cliente = tcpServer.accept();
                    pool.execute(() -> autenticarCliente(cliente));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        DatagramSocket udpSocket = new DatagramSocket();
        InetAddress grupo = InetAddress.getByName(GRUPO_IP);
        Scanner scanner = new Scanner(System.in);

        System.out.println("[Multicast] Pronto para transmitir em " + GRUPO_IP + ":" + PORTA_MULTICAST);
        while (true) {
            System.out.println("\nEscolha o tipo de aviso: [1] NOTIFICACAO | [2] ALERTA | [3] ATUALIZACAO");
            String op = scanner.nextLine();
            String tipo = op.equals("2") ? "ALERTA" : (op.equals("3") ? "ATUALIZACAO" : "NOTIFICACAO");

            System.out.print("Digite a mensagem para os clientes do Sebo: ");
            String texto = scanner.nextLine();

            String mensagemSegura = texto.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\r", "\\r")
                    .replace("\n", "\\n");
            String json = String.format(
                "{\"tipo\": \"%s\", \"mensagem\": \"%s\", \"timestamp\": %d}",
                tipo, mensagemSegura, System.currentTimeMillis()
            );

            pool.execute(() -> {
                try {
                    byte[] buffer = json.getBytes(StandardCharsets.UTF_8);
                    DatagramPacket pacote = new DatagramPacket(buffer, buffer.length, grupo, PORTA_MULTICAST);
                    udpSocket.send(pacote);
                    System.out.println("[Enviado Multicast] " + json);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    private static void autenticarCliente(Socket socket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            String credenciais = in.readLine();
            if (credenciais != null && credenciais.endsWith(":1234")) {
                out.println("AUTH_OK;" + GRUPO_IP + ";" + PORTA_MULTICAST);
                System.out.println("[TCP Auth] Cliente autenticado: " + credenciais.split(":")[0]);
            } else {
                out.println("AUTH_ERRO");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
