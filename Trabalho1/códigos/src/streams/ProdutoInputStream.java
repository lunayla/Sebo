package streams;

import model.*;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

public class ProdutoInputStream extends InputStream {
    private DataInputStream in;

    public ProdutoInputStream(InputStream origem) {
        this.in = new DataInputStream(origem);
    }

    @Override
    public int read() throws IOException {
        return in.read();
    }

    public Produto[] lerProdutos() throws IOException {
        int qtd = in.readInt();
        if (qtd < 0 || qtd > 100000) {
            throw new IOException("Quantidade de produtos inválida no fluxo: " + qtd);
        }
        Produto[] lista = new Produto[qtd];

        for (int i = 0; i < qtd; i++) {
            String tipo = in.readUTF();
            int id = in.readInt();
            String titulo = in.readUTF();
            double preco = in.readDouble();
            String estado = in.readUTF();
            String detalhe1 = in.readUTF();

            switch (tipo) {
                case "LIVRO":
                    String isbn = in.readUTF();
                    lista[i] = new Livro(id, titulo, preco, estado, detalhe1, isbn);
                    break;
                case "APOSTILA":
                    lista[i] = new Apostila(id, titulo, preco, estado, detalhe1);
                    break;
                case "CD":
                    lista[i] = new CD(id, titulo, preco, estado, detalhe1);
                    break;
                case "EBOOK":
                    lista[i] = new Ebook(id, titulo, preco, detalhe1);
                    break;
                default:
                    throw new IOException("Tipo de produto desconhecido no fluxo: " + tipo);
            }
        }
        return lista;
    }
}
