package client;

import protocol.MensagemRR;
import rmi.SeboDispatcherRemote;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Proxy do Cliente implementando doOperation() conforme Seção 5.2 do Coulouris.
 */
public class ClienteProxyRR {
    private final SeboDispatcherRemote servidorStub;
    private final AtomicInteger geradorRequestId = new AtomicInteger(1);

    public ClienteProxyRR(SeboDispatcherRemote servidorStub) {
        this.servidorStub = servidorStub;
    }

    /**
     * Primitiva doOperation(RemoteObjectRef o, String methodId, byte[] arguments)
     */
    public byte[] doOperation(String objectReference, String methodId, byte[] arguments) throws Exception {
        int reqId = geradorRequestId.getAndIncrement();

        // 1. Empacota mensagem Request (messageType = 0)
        MensagemRR request = new MensagemRR(
                MensagemRR.REQUEST,
                reqId,
                objectReference,
                methodId,
                arguments
        );
        byte[] bytesRequest = request.empacotar();

        // 2. Invoca operação remota via RMI (bloqueia até receber Reply)
        byte[] bytesReply = servidorStub.enviarMensagemProtocolo(bytesRequest);

        // 3. Desempacota mensagem Reply (messageType = 1)
        MensagemRR reply = MensagemRR.desempacotar(bytesReply);
        if (reply.getMessageType() != MensagemRR.REPLY || reply.getRequestId() != reqId) {
            throw new IllegalStateException("Resposta invalida ou fora de ordem no protocolo Request-Reply.");
        }
        return reply.getArguments();
    }
}
