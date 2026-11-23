package serializacao;

import model.*;
import service.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.stream.Collectors;

public class ServidorSerializacaoTCP {
    public static void main(String[] args) throws Exception {
        Loja loja = new Loja("Sebo Universitário Quixadá");
        EstoqueService estoque = new EstoqueService(loja);
        NegociacaoService negociacao = new NegociacaoService(loja);

        estoque.cadastrarProduto(new Livro(1, "Dom Casmurro", 25.0, "Seminovo", "Machado de Assis", "111"));
        estoque.cadastrarProduto(new Apostila(2, "Cálculo I", 15.0, "Usado", "Matemática"));
        estoque.cadastrarProduto(new Ebook(3, "Padrões de Projeto", 50.0, "PDF"));

        try (ServerSocket server = new ServerSocket(5002)) {
            System.out.println("Servidor de Serialização TCP rodando na porta 5002...");
            while (true) {
                try (Socket client = server.accept();
                     DataInputStream in = new DataInputStream(client.getInputStream());
                     DataOutputStream out = new DataOutputStream(client.getOutputStream())) {

                    int tamanhoReq = in.readInt();
                    byte[] bytesReq = new byte[tamanhoReq];
                    in.readFully(bytesReq);
                    MensagemRequest req = MensagemRequest.desempacotar(bytesReq);

                    MensagemReply reply;
                    switch (req.getOperacao()) {
                        case "LISTAR":
                            String lista = estoque.listarTodos().stream()
                                    .map(Produto::toString)
                                    .collect(Collectors.joining("\n"));
                            reply = new MensagemReply(true, lista.isEmpty() ? "Acervo vazio." : lista);
                            break;
                        case "COMPRAR":
                            String resVenda = negociacao.realizarVenda(req.getIdProduto());
                            reply = new MensagemReply(!resVenda.startsWith("ERRO"), resVenda);
                            break;
                        case "AVALIAR_TROCA":
                            String resTroca = negociacao.avaliarTroca(req.getIdProduto());
                            reply = new MensagemReply(!resTroca.startsWith("ERRO"), resTroca);
                            break;
                        default:
                            reply = new MensagemReply(false, "Operação inválida.");
                    }

                    byte[] bytesReply = reply.empacotar();
                    out.writeInt(bytesReply.length);
                    out.write(bytesReply);
                    out.flush();
                }
            }
        }
    }
}
