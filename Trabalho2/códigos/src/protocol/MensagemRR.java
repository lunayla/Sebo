package protocol;

import java.io.*;

/**
 * Estrutura de Mensagem do Protocolo Requisição-Resposta (Coulouris, Seção 5.2):
 * - messageType (int: 0 = Request, 1 = Reply)
 * - requestId (int)
 * - objectReference (String)
 * - methodId (String)
 * - arguments (byte[])
 */
public class MensagemRR implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final int REQUEST = 0;
    public static final int REPLY = 1;

    private int messageType;
    private int requestId;
    private String objectReference;
    private String methodId;
    private byte[] arguments;

    public MensagemRR(int messageType, int requestId, String objectReference, String methodId, byte[] arguments) {
        this.messageType = messageType;
        this.requestId = requestId;
        this.objectReference = objectReference;
        this.methodId = methodId;
        this.arguments = arguments;
    }

    public int getMessageType() { return messageType; }
    public int getRequestId() { return requestId; }
    public String getObjectReference() { return objectReference; }
    public String getMethodId() { return methodId; }
    public byte[] getArguments() { return arguments; }

    public byte[] empacotar() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (DataOutputStream dos = new DataOutputStream(baos)) {
            dos.writeInt(messageType);
            dos.writeInt(requestId);
            dos.writeUTF(objectReference != null ? objectReference : "");
            dos.writeUTF(methodId != null ? methodId : "");
            if (arguments != null) {
                dos.writeInt(arguments.length);
                dos.write(arguments);
            } else {
                dos.writeInt(0);
            }
        }
        return baos.toByteArray();
    }

    public static MensagemRR desempacotar(byte[] dados) throws IOException {
        try (DataInputStream dis = new DataInputStream(new ByteArrayInputStream(dados))) {
            int type = dis.readInt();
            int reqId = dis.readInt();
            String objRef = dis.readUTF();
            String mId = dis.readUTF();
            int len = dis.readInt();
            if (type != REQUEST && type != REPLY) throw new IOException("Tipo de mensagem inválido: " + type);
            if (len < 0 || len > 10_000_000) throw new IOException("Tamanho de argumentos inválido: " + len);
            byte[] args = new byte[len];
            if (len > 0) dis.readFully(args);
            if (dis.available() != 0) throw new IOException("Bytes excedentes na mensagem Request-Reply.");
            return new MensagemRR(type, reqId, objRef, mId, args);
        }
    }
}
