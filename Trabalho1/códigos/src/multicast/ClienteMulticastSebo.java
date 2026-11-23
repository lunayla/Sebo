package multicast;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class ClienteMulticastSebo {
    private static volatile boolean ativo = true;

    @SuppressWarnings("deprecation")
    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Usuário: ");
        String usuario = scanner.nextLine();
        System.out.print("Senha (use 1234): ");
        String senha = scanner.nextLine();

        String hostServidor = System.getenv().getOrDefault("SEBO_SERVER_HOST", "localhost");
        String grupoIp;
        int portaMulticast;
        try (Socket tcpSocket = new Socket(hostServidor, 5003);
             PrintWriter out = new PrintWriter(tcpSocket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(tcpSocket.getInputStream()))) {

            out.println(usuario + ":" + senha);
            String resposta = in.readLine();
            if (resposta == null || !resposta.startsWith("AUTH_OK")) {
                System.out.println("Falha na autenticação TCP. Encerrando.");
                return;
            }
            String[] partes = resposta.split(";");
            grupoIp = partes[1];
            portaMulticast = Integer.parseInt(partes[2]);
            System.out.println("Autenticado! Conectando ao grupo Multicast " + grupoIp + ":" + portaMulticast);
        }

        MulticastSocket multicastSocket = new MulticastSocket(portaMulticast);
        InetAddress grupo = InetAddress.getByName(grupoIp);
        multicastSocket.joinGroup(grupo);

        Thread listenerThread = new Thread(() -> {
            byte[] buffer = new byte[1024];
            while (ativo) {
                try {
                    DatagramPacket pacote = new DatagramPacket(buffer, buffer.length);
                    multicastSocket.receive(pacote);
                    String msgJson = new String(pacote.getData(), 0, pacote.getLength(), StandardCharsets.UTF_8);
                    System.out.println("\n[MURAL DO SEBO - TEMPO REAL] " + msgJson);
                    System.out.print("> ");
                } catch (IOException e) {
                    if (ativo) System.out.println("Socket multicast encerrado.");
                }
            }
        });
        listenerThread.start();

        System.out.println("Você entrou no grupo! Digite 'sair' a qualquer momento para executar leaveGroup().");
        while (ativo) {
            System.out.print("> ");
            String comando = scanner.nextLine();
            if (comando.equalsIgnoreCase("sair")) {
                ativo = false;
                multicastSocket.leaveGroup(grupo);
                multicastSocket.close();
                System.out.println("Você saiu do grupo multicast (leaveGroup executado).");
            }
        }
    }
}
