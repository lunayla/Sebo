package streams;

import model.*;
import java.net.Socket;

public class ClienteStreamTCP {
    public static void main(String[] args) throws Exception {
        Produto[] envio = {
            new Livro(10, "Clean Code", 70.0, "Seminovo", "Robert Martin", "978-0132350884"),
            new Ebook(11, "Algoritmos", 35.0, "PDF")
        };

        String host = System.getenv().getOrDefault("SEBO_SERVER_HOST", "localhost");
        try (Socket socket = new Socket(host, 5001)) {
            ProdutoOutputStream pos = new ProdutoOutputStream(socket.getOutputStream(), envio, envio.length);
            pos.enviarProdutos();
            System.out.println("Produtos enviados ao servidor TCP com sucesso!");
        }
    }
}
