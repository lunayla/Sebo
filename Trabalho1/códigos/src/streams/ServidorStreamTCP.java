package streams;

import model.Produto;
import java.net.ServerSocket;
import java.net.Socket;

public class ServidorStreamTCP {
    public static void main(String[] args) throws Exception {
        try (ServerSocket server = new ServerSocket(5001)) {
            System.out.println("Servidor Stream TCP aguardando na porta 5001...");
            try (Socket client = server.accept()) {
                ProdutoInputStream pis = new ProdutoInputStream(client.getInputStream());
                Produto[] recebidos = pis.lerProdutos();
                System.out.println("Produtos recebidos via Socket TCP:");
                for (Produto p : recebidos) {
                    System.out.println(" -> " + p);
                }
            }
        }
    }
}
