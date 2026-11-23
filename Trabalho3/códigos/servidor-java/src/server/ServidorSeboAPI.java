package server;

import com.sun.net.httpserver.HttpServer;
import resources.*;
import service.SeboRepository;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class ServidorSeboAPI {
    public static void main(String[] args) throws Exception {
        int porta = 8080;
        SeboRepository repository = new SeboRepository();

        HttpServer server = HttpServer.create(new InetSocketAddress(porta), 0);

        // Registro dos 4 Objetos Distribuídos (Endpoints REST)
        server.createContext("/api/auth/login", new AuthResource());
        server.createContext("/api/produtos", new AcervoResource(repository));
        server.createContext("/api/negociacoes", new NegociacaoResource(repository));
        server.createContext("/api/notificacoes", new MuralResource(repository));

        // Suporte concorrente a múltiplos clientes (Python, Node.js, etc.)
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();

        System.out.println("===============================================================");
        System.out.println(" Web Service REST do Sebo iniciado em http://localhost:" + porta);
        System.out.println(" Objetos Distribuídos ativos:");
        System.out.println("  1. AuthResource       -> POST /api/auth/login");
        System.out.println("  2. AcervoResource     -> GET, POST /api/produtos");
        System.out.println("  3. NegociacaoResource -> POST /api/negociacoes/{comprar|trocar}");
        System.out.println("  4. MuralResource      -> GET, POST /api/notificacoes");
        System.out.println("===============================================================");
    }
}
