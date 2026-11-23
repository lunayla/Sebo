package model;

public class Notificacao {
    private int id;
    private String tipo; // NOTIFICACAO, ALERTA, ATUALIZACAO
    private String mensagem;
    private long timestamp;
    private ClienteUsuario remetente; // Agregação: Notificacao "tem-um" ClienteUsuario

    public Notificacao(int id, String tipo, String mensagem, long timestamp, ClienteUsuario remetente) {
        this.id = id;
        this.tipo = tipo;
        this.mensagem = mensagem;
        this.timestamp = timestamp;
        this.remetente = remetente;
    }

    public int getId() { return id; }
    public String getTipo() { return tipo; }
    public String getMensagem() { return mensagem; }
    public long getTimestamp() { return timestamp; }
    public ClienteUsuario getRemetente() { return remetente; }
}
