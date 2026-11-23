package server;

import model.Loja;
import model.Notificacao;
import protocol.MensagemRR;
import rmi.SeboDispatcherRemote;
import service.SeboServicoLocal;
import util.JsonHelper;
import pubsub.PubSubPublisher;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Implementação do servidor seguindo o protocolo Requisição-Resposta (Seção 5.2):
 * - getRequest()
 * - select object & execute method
 * - sendReply()
 */
public class SeboDispatcherImpl extends UnicastRemoteObject
        implements SeboDispatcherRemote {

    private static final long serialVersionUID = 1L;

    private final SeboServicoLocal servicoLocal;
    private final BlockingQueue<byte[]> filaRequisicoes =
            new LinkedBlockingQueue<>();
    private final Map<Integer, CompletableFuture<byte[]>> respostasPendentes =
            new ConcurrentHashMap<>();
    private final AtomicInteger sequenciaInterna = new AtomicInteger(1);

    private final ExecutorService executorRequisicoes =
            Executors.newFixedThreadPool(
                    Math.max(4, Runtime.getRuntime().availableProcessors())
            );


    public SeboDispatcherImpl(Loja loja) throws RemoteException {
        super();

        this.servicoLocal = new SeboServicoLocal(loja);

        Thread loopProtocolo = new Thread(this::executarCicloServidor);
        loopProtocolo.setDaemon(true);
        loopProtocolo.start();
    }

    @Override
    public byte[] enviarMensagemProtocolo(byte[] requestEmpacotado)
            throws RemoteException {

        try {
            MensagemRR req = MensagemRR.desempacotar(requestEmpacotado);

            if (req.getMessageType() != MensagemRR.REQUEST) {
                throw new IOException(
                        "O servidor esperava uma mensagem Request."
                );
            }

            int idInterno = sequenciaInterna.getAndIncrement();
            CompletableFuture<byte[]> futuroReply =
                    new CompletableFuture<>();

            respostasPendentes.put(idInterno, futuroReply);

            MensagemRR reqInterna = new MensagemRR(
                    MensagemRR.REQUEST,
                    idInterno,
                    req.getObjectReference(),
                    req.getMethodId(),
                    req.getArguments()
            );

            filaRequisicoes.put(reqInterna.empacotar());

            byte[] replyInterna =
                    futuroReply.get(30, TimeUnit.SECONDS);

            MensagemRR reply = MensagemRR.desempacotar(replyInterna);

            MensagemRR replyCliente = new MensagemRR(
                    MensagemRR.REPLY,
                    req.getRequestId(),
                    reply.getObjectReference(),
                    reply.getMethodId(),
                    reply.getArguments()
            );

            return replyCliente.empacotar();

        } catch (Exception e) {
            throw new RemoteException(
                    "Erro no protocolo Request-Reply do servidor", e
            );
        }
    }

    public byte[] getRequest() throws InterruptedException {
        return filaRequisicoes.take();
    }

    public void sendReply(byte[] reply, int requestId) {
        CompletableFuture<byte[]> futuro =
                respostasPendentes.remove(requestId);

        if (futuro != null) {
            futuro.complete(reply);
        }
    }

    private void executarCicloServidor() {
        while (true) {
            try {
                byte[] bytesReq = getRequest();
                MensagemRR req = MensagemRR.desempacotar(bytesReq);

                System.out.printf(
                        "[Servidor 5.2] Request recebido | ID=%d | Obj=%s | Método=%s%n",
                        req.getRequestId(),
                        req.getObjectReference(),
                        req.getMethodId()
                );

                executorRequisicoes.execute(
                        () -> processarRequisicao(req)
                );

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void processarRequisicao(MensagemRR req) {
        try {
            byte[] resultadoBytes = despacharMetodo(
                    req.getObjectReference(),
                    req.getMethodId(),
                    req.getArguments()
            );

            MensagemRR replyMsg = new MensagemRR(
                    MensagemRR.REPLY,
                    req.getRequestId(),
                    req.getObjectReference(),
                    req.getMethodId(),
                    resultadoBytes
            );

            sendReply(replyMsg.empacotar(), req.getRequestId());

        } catch (Exception e) {
            CompletableFuture<byte[]> futuro =
                    respostasPendentes.remove(req.getRequestId());

            if (futuro != null) {
                futuro.completeExceptionally(e);
            }

            e.printStackTrace();
        }
    }

    private byte[] despacharMetodo(
            String objectReference,
            String methodId,
            byte[] args
    ) {
        if (!"SeboService".equals(objectReference)) {
            return JsonHelper.respostaJson(
                    false,
                    "Objeto remoto desconhecido: " + objectReference,
                    null
            ).getBytes(StandardCharsets.UTF_8);
        }

        // Toda operação, exceto o login, exige uma sessão válida.
        if (!"autenticar".equals(methodId)) {
            Map<String, String> parametros =
                    JsonHelper.parseObjetoSimples(
                            new String(
                                    args == null ? new byte[0] : args,
                                    StandardCharsets.UTF_8
                            )
                    );

            String perfil =
                    servicoLocal.obterPerfil(parametros.get("token"));

            if (perfil == null) {
                return JsonHelper.respostaJson(
                        false,
                        "Sessão inválida ou expirada. Faça login novamente.",
                        null
                ).getBytes(StandardCharsets.UTF_8);
            }

            // Somente ADMIN pode cadastrar produtos e publicar avisos.
            if (("cadastrarProduto".equals(methodId)
                    || "publicarNotificacao".equals(methodId))
                    && !"ADMIN".equals(perfil)) {

                return JsonHelper.respostaJson(
                        false,
                        "Acesso negado: esta operação é exclusiva do administrador.",
                        null
                ).getBytes(StandardCharsets.UTF_8);
            }
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

                String notifJson =
                        JsonHelper.notificacaoParaJson(notif);

                try {
                    String brokerHost = System.getenv().getOrDefault("SEBO_BROKER_HOST", "localhost");
                    int brokerPort = Integer.parseInt(System.getenv().getOrDefault("SEBO_BROKER_PORT", "5050"));
                    new PubSubPublisher(brokerHost, brokerPort).publish("avisos-gerais", notifJson);
                    return JsonHelper.respostaJson(
                            true,
                            "Evento publicado no tópico avisos-gerais. O Broker fará a distribuição aos assinantes.",
                            notifJson
                    ).getBytes(StandardCharsets.UTF_8);
                } catch (IOException e) {
                    return JsonHelper.respostaJson(
                            false,
                            "Não foi possível publicar: Broker Pub-Sub indisponível.",
                            null
                    ).getBytes(StandardCharsets.UTF_8);
                }

            default:
                return JsonHelper.respostaJson(
                        false,
                        "Método remoto inexistente: " + methodId,
                        null
                ).getBytes(StandardCharsets.UTF_8);
        }
    }

}
