package rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Interface remota passada POR REFERÊNCIA pelo cliente ao servidor
 * para receber notificações em tempo real (substituindo o Multicast do Trabalho 1).
 */
public interface ClienteCallbackRemote extends Remote {
    void receberNotificacaoTempoReal(byte[] notificacaoJsonBytes) throws RemoteException;
    String getNomeCliente() throws RemoteException;
}
