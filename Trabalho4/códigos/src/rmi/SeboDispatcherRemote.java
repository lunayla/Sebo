package rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

/** Interface RMI usada para operações síncronas do sebo. Eventos são distribuídos pelo Broker Pub-Sub. */
public interface SeboDispatcherRemote extends Remote {
    byte[] enviarMensagemProtocolo(byte[] requestEmpacotado) throws RemoteException;
}
