package serializacao;

import java.io.*;
import java.net.Socket;

public class ClienteSerializacaoTCP {
    private static MensagemReply enviarRequisicao(MensagemRequest req) throws Exception {
        String host = System.getenv().getOrDefault("SEBO_SERVER_HOST", "localhost");
        try (Socket socket = new Socket(host, 5002);
             DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             DataInputStream in = new DataInputStream(socket.getInputStream())) {

            byte[] dadosReq = req.empacotar();
            out.writeInt(dadosReq.length);
            out.write(dadosReq);
            out.flush();

            int tamanhoReply = in.readInt();
            byte[] dadosReply = new byte[tamanhoReply];
            in.readFully(dadosReply);
            return MensagemReply.desempacotar(dadosReply);
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("--- 1. Listando acervo ---");
        System.out.println(enviarRequisicao(new MensagemRequest("LISTAR", 0)).getPayload());

        System.out.println("\n--- 2. Avaliando troca do E-book (ID 3) ---");
        System.out.println(enviarRequisicao(new MensagemRequest("AVALIAR_TROCA", 3)).getPayload());

        System.out.println("\n--- 3. Avaliando troca do Livro (ID 1) ---");
        System.out.println(enviarRequisicao(new MensagemRequest("AVALIAR_TROCA", 1)).getPayload());

        System.out.println("\n--- 4. Comprando Livro (ID 1) ---");
        System.out.println(enviarRequisicao(new MensagemRequest("COMPRAR", 1)).getPayload());
    }
}
