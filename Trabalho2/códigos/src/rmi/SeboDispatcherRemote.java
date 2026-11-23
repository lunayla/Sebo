package rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Interface remota do servidor RMI que transporta mensagens empacotadas
 * do protocolo Requisição-Resposta (Seção 5.2) e gerencia referências remotas de clientes.
 */
public interface SeboDispatcherRemote extends Remote {
    byte[] enviarMensagemProtocolo(byte[] requestEmpacotado) throws RemoteException;
    void registrarCallbackCliente(ClienteCallbackRemote clienteRef) throws RemoteException;
    void removerCallbackCliente(ClienteCallbackRemote clienteRef) throws RemoteException;
}
