package client;

import rmi.ClienteCallbackRemote;
import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

/**
 * Objeto remoto do cliente (passado por referência ao servidor)
 * que recebe notificações em tempo real em thread assíncrona do RMI.
 */
public class ClienteCallbackImpl extends UnicastRemoteObject implements ClienteCallbackRemote {
    private static final long serialVersionUID = 1L;
    private final String nomeCliente;

    public ClienteCallbackImpl(String nomeCliente) throws RemoteException {
        super();
        this.nomeCliente = nomeCliente;
    }

    @Override
    public void receberNotificacaoTempoReal(byte[] notificacaoJsonBytes) throws RemoteException {
        String json = new String(notificacaoJsonBytes, StandardCharsets.UTF_8);
        System.out.println("\n\n>>> [MURAL DO SEBO - TEMPO REAL] " + json);
        System.out.print("Escolha uma opção: ");
    }

    @Override
    public String getNomeCliente() throws RemoteException {
        return nomeCliente;
    }
}
