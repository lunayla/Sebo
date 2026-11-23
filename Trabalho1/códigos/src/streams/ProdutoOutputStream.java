package streams;

import model.*;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class ProdutoOutputStream extends OutputStream {
    private DataOutputStream out;
    private Produto[] produtos;
    private int numObjetos;

    public ProdutoOutputStream(OutputStream destino, Produto[] produtos, int numObjetos) {
        if (destino == null || produtos == null) {
            throw new IllegalArgumentException("Destino e lista de produtos são obrigatórios.");
        }
        if (numObjetos < 0 || numObjetos > produtos.length) {
            throw new IllegalArgumentException("Quantidade de objetos inválida.");
        }
        this.out = new DataOutputStream(destino);
        this.produtos = produtos;
        this.numObjetos = numObjetos;
    }

    @Override
    public void write(int b) throws IOException {
        out.write(b);
    }

    public void enviarProdutos() throws IOException {
        out.writeInt(numObjetos);
        for (int i = 0; i < numObjetos; i++) {
            Produto p = produtos[i];
            out.writeUTF(p.getTipo());
            out.writeInt(p.getId());
            out.writeUTF(p.getTitulo());
            out.writeDouble(p.getPreco());
            out.writeUTF(p.getEstadoConservacao());
            // Campo específico da subclasse, para não perder dados na leitura.
            if (p instanceof Livro) {
                Livro livro = (Livro) p;
                out.writeUTF(livro.getAutor());
                out.writeUTF(livro.getIsbn());
            } else if (p instanceof Apostila) {
                out.writeUTF(((Apostila) p).getDisciplina());
            } else if (p instanceof CD) {
                out.writeUTF(((CD) p).getArtista());
            } else if (p instanceof Ebook) {
                out.writeUTF(((Ebook) p).getFormato());
            } else {
                throw new IOException("Tipo de produto não suportado: " + p.getClass().getName());
            }
        }
        out.flush();
    }
}
