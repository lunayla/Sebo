package streams;

import model.*;
import java.io.*;

public class TesteStreamsLocal {
    public static void main(String[] args) throws IOException {
        Produto[] acervo = {
            new Livro(1, "Sistemas Distribuídos - Coulouris", 85.0, "Seminovo", "George Coulouris", "978-8582600535"),
            new Apostila(2, "Resumo Redes e Sockets", 20.0, "Novo", "Sistemas Distribuídos"),
            new CD(3, "Legião Urbana - Acústico", 30.0, "Usado", "Legião Urbana"),
            new Ebook(4, "Java Concurrency in Practice", 45.0, "EPUB")
        };

        System.out.println("=== Teste 1(b): Enviando bytes para System.out ===");
        PrintStream saidaOriginal = System.out;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try {
            System.setOut(new PrintStream(buffer));
            ProdutoOutputStream posConsole = new ProdutoOutputStream(System.out, acervo, 2);
            posConsole.enviarProdutos();
        } finally {
            System.setOut(saidaOriginal);
        }
        System.out.println("Dados enviados diretamente para System.out (saída capturada para não poluir o terminal).");
        System.out.println("Bytes enviados: " + buffer.size());

        System.out.println("\n=== Teste 1(c): Gravando em arquivo 'produtos.dat' ===");
        try (FileOutputStream fos = new FileOutputStream("produtos.dat")) {
            ProdutoOutputStream posArquivo = new ProdutoOutputStream(fos, acervo, 3);
            posArquivo.enviarProdutos();
        }
        System.out.println("Arquivo gravado com sucesso.");

        System.out.println("\n=== Teste 2(c): Lendo de 'produtos.dat' via FileInputStream ===");
        try (FileInputStream fis = new FileInputStream("produtos.dat")) {
            ProdutoInputStream pisArquivo = new ProdutoInputStream(fis);
            Produto[] lidos = pisArquivo.lerProdutos();
            for (Produto p : lidos) {
                System.out.println("Lido do arquivo -> " + p);
                if (p instanceof Livro) {
                    System.out.println("   Autor: " + ((Livro) p).getAutor() + " | ISBN: " + ((Livro) p).getIsbn());
                } else if (p instanceof Apostila) {
                    System.out.println("   Disciplina: " + ((Apostila) p).getDisciplina());
                } else if (p instanceof CD) {
                    System.out.println("   Artista: " + ((CD) p).getArtista());
                } else if (p instanceof Ebook) {
                    System.out.println("   Formato: " + ((Ebook) p).getFormato());
                }
            }
        }

        System.out.println("\n=== Teste 2(b): Lendo de System.in ===");
        InputStream originalIn = System.in;
        System.setIn(new ByteArrayInputStream(buffer.toByteArray()));
        ProdutoInputStream pisSysIn = new ProdutoInputStream(System.in);
        for (Produto p : pisSysIn.lerProdutos()) {
            System.out.println("Lido do System.in -> " + p);
        }
        System.setIn(originalIn);
    }
}
