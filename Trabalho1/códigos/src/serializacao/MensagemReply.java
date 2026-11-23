package serializacao;

import java.io.*;

public class MensagemReply implements Serializable {
    private static final long serialVersionUID = 1L;
    private boolean sucesso;
    private String payload;

    public MensagemReply(boolean sucesso, String payload) {
        this.sucesso = sucesso;
        this.payload = payload;
    }

    public boolean isSucesso() { return sucesso; }
    public String getPayload() { return payload; }

    public byte[] empacotar() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(this);
        }
        return baos.toByteArray();
    }

    public static MensagemReply desempacotar(byte[] dados) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(dados))) {
            return (MensagemReply) ois.readObject();
        }
    }
}
