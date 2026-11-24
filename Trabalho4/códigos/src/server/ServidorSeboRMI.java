package server;

import model.Loja;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class ServidorSeboRMI {
    public static void main(String[] args) {
        try {
            String hostAnunciado = System.getenv().getOrDefault("SEBO_SERVER_HOST", "localhost");
            System.setProperty("java.rmi.server.hostname", hostAnunciado);
            Loja loja = new Loja("Sistema de Sebo Virtual");
            SeboDispatcherImpl dispatcher = new SeboDispatcherImpl(loja);

            Registry registry = LocateRegistry.createRegistry(1099);
            registry.rebind("SeboDispatcher", dispatcher);

            System.out.println("==========================================================");
            System.out.println(" Servidor RMI do Sebo iniciado na porta 1099!");
            System.out.println("==========================================================");
        } catch (Exception e) {
            System.err.println("Erro ao iniciar Servidor RMI: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
