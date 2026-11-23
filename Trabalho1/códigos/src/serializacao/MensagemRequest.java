package serializacao;

import java.io.*;

public class MensagemRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private String operacao;
    private int idProduto;

    public MensagemRequest(String operacao, int idProduto) {
        this.operacao = operacao;
        this.idProduto = idProduto;
    }

    public String getOperacao() { return operacao; }
    public int getIdProduto() { return idProduto; }

    public byte[] empacotar() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(this);
        }
        return baos.toByteArray();
    }

    public static MensagemRequest desempacotar(byte[] dados) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(dados))) {
            return (MensagemRequest) ois.readObject();
        }
    }
}
