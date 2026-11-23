package server;

import model.Loja;
import model.Notificacao;
import protocol.MensagemRR;
import rmi.ClienteCallbackRemote;
import rmi.SeboDispatcherRemote;
import service.SeboServicoLocal;
import util.JsonHelper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Implementação do servidor seguindo o protocolo Requisição-Resposta (Seção 5.2):
 * - getRequest()
 * - select object & execute method
 * - sendReply()
 */
public class SeboDispatcherImpl extends UnicastRemoteObject implements SeboDispatcherRemote {
    private static final long serialVersionUID = 1L;

    private final SeboServicoLocal servicoLocal;
    private final BlockingQueue<byte[]> filaRequisicoes = new LinkedBlockingQueue<>();
    private final Map<Integer, CompletableFuture<byte[]>> respostasPendentes = new ConcurrentHashMap<>();
    private final AtomicInteger sequenciaInterna = new AtomicInteger(1);
    private final ExecutorService executorRequisicoes = Executors.newFixedThreadPool(
            Math.max(4, Runtime.getRuntime().availableProcessors()));
    private final List<ClienteCallbackRemote> clientesConectados = new CopyOnWriteArrayList<>();
    private final ExecutorService executorNotificacoes = Executors.newCachedThreadPool();

    public SeboDispatcherImpl(Loja loja) throws RemoteException {
        super();
        this.servicoLocal = new SeboServicoLocal(loja);

        // Thread dedicada do servidor executando o ciclo getRequest -> execute -> sendReply
        Thread loopProtocolo = new Thread(this::executarCicloServidor);
        loopProtocolo.setDaemon(true);
        loopProtocolo.start();
    }

    @Override
    public byte[] enviarMensagemProtocolo(byte[] requestEmpacotado) throws RemoteException {
        try {
            MensagemRR req = MensagemRR.desempacotar(requestEmpacotado);
            if (req.getMessageType() != MensagemRR.REQUEST) {
                throw new IOException("O servidor esperava uma mensagem Request.");
            }
            int idInterno = sequenciaInterna.getAndIncrement();
            CompletableFuture<byte[]> futuroReply = new CompletableFuture<>();
            respostasPendentes.put(idInterno, futuroReply);

            // O ID interno é exclusivo no servidor; o ID original do cliente será restaurado na Reply.
            MensagemRR reqInterna = new MensagemRR(MensagemRR.REQUEST, idInterno,
                    req.getObjectReference(), req.getMethodId(), req.getArguments());
            filaRequisicoes.put(reqInterna.empacotar());

            byte[] replyInterna = futuroReply.get(30, TimeUnit.SECONDS);
            MensagemRR reply = MensagemRR.desempacotar(replyInterna);
            MensagemRR replyCliente = new MensagemRR(MensagemRR.REPLY, req.getRequestId(),
                    reply.getObjectReference(), reply.getMethodId(), reply.getArguments());
            return replyCliente.empacotar();
        } catch (Exception e) {
            throw new RemoteException("Erro no protocolo Request-Reply do servidor", e);
        }
    }

    /**
     * Primitiva getRequest() conforme Seção 5.2 do livro texto.
     */
    public byte[] getRequest() throws InterruptedException {
        return filaRequisicoes.take();
    }

    /**
     * Primitiva sendReply() conforme Seção 5.2 do livro texto.
     */
    public void sendReply(byte[] reply, int requestId) {
        CompletableFuture<byte[]> futuro = respostasPendentes.remove(requestId);
        if (futuro != null) {
            futuro.complete(reply);
        }
    }

    private void executarCicloServidor() {
        while (true) {
            try {
                // 1. getRequest
                byte[] bytesReq = getRequest();
                MensagemRR req = MensagemRR.desempacotar(bytesReq);

                System.out.printf("[Servidor 5.2] Request recebido | ID=%d | Obj=%s | Método=%s%n",
                        req.getRequestId(), req.getObjectReference(), req.getMethodId());

                // Cada requisição é processada em uma tarefa própria, permitindo clientes concorrentes.
                executorRequisicoes.execute(() -> processarRequisicao(req));

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void processarRequisicao(MensagemRR req) {
        try {
            byte[] resultadoBytes = despacharMetodo(req.getObjectReference(), req.getMethodId(), req.getArguments());
            MensagemRR replyMsg = new MensagemRR(MensagemRR.REPLY, req.getRequestId(),
                    req.getObjectReference(), req.getMethodId(), resultadoBytes);
            sendReply(replyMsg.empacotar(), req.getRequestId());
        } catch (Exception e) {
            CompletableFuture<byte[]> futuro = respostasPendentes.remove(req.getRequestId());
            if (futuro != null) futuro.completeExceptionally(e);
            e.printStackTrace();
        }
    }

    private byte[] despacharMetodo(String objectReference, String methodId, byte[] args) {
        if (!"SeboService".equals(objectReference)) {
            return JsonHelper.respostaJson(false, "Objeto remoto desconhecido: " + objectReference, null)
                    .getBytes(StandardCharsets.UTF_8);
        }

        switch (methodId) {
            case "autenticar":
                return servicoLocal.autenticar(args);
            case "listarAcervo":
                return servicoLocal.listarAcervo();
            case "cadastrarProduto":
                return servicoLocal.cadastrarProduto(args);
            case "realizarVenda":
                return servicoLocal.realizarVenda(args);
            case "avaliarTroca":
                return servicoLocal.avaliarTroca(args);
            case "publicarNotificacao":
                Notificacao notif = servicoLocal.criarNotificacao(args);
                String notifJson = JsonHelper.notificacaoParaJson(notif);
                difundirNotificacaoTempoReal(notifJson.getBytes(StandardCharsets.UTF_8));
                return JsonHelper.respostaJson(true, "Notificação publicada para todos os clientes.", notifJson)
                        .getBytes(StandardCharsets.UTF_8);
            default:
                return JsonHelper.respostaJson(false, "Método remoto inexistente: " + methodId, null)
                        .getBytes(StandardCharsets.UTF_8);
        }
    }

    private void difundirNotificacaoTempoReal(byte[] payloadJson) {
        for (ClienteCallbackRemote cliente : clientesConectados) {
            executorNotificacoes.execute(() -> {
                try {
                    cliente.receberNotificacaoTempoReal(payloadJson);
                } catch (RemoteException e) {
                    clientesConectados.remove(cliente);
                }
            });
        }
    }

    @Override
    public void registrarCallbackCliente(ClienteCallbackRemote clienteRef) throws RemoteException {
        clientesConectados.add(clienteRef);
        System.out.println("[RMI Callback] Cliente inscrito nas notificações: " + clienteRef.getNomeCliente());
    }

    @Override
    public void removerCallbackCliente(ClienteCallbackRemote clienteRef) throws RemoteException {
        clientesConectados.remove(clienteRef);
        System.out.println("[RMI Callback] Cliente removido das notificações.");
    }
}
