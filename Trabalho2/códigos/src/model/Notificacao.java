package model;

import java.io.Serializable;

public class Notificacao implements Serializable {
    private static final long serialVersionUID = 1L;
    private String tipo; // "NOTIFICACAO", "ALERTA", "ATUALIZACAO"
    private String mensagem;
    private long timestamp;
    private ClienteUsuario remetente; // Agregação: Notificacao "tem-um" ClienteUsuario

    public Notificacao(String tipo, String mensagem, long timestamp, ClienteUsuario remetente) {
        this.tipo = tipo;
        this.mensagem = mensagem;
        this.timestamp = timestamp;
        this.remetente = remetente;
    }

    public String getTipo() { return tipo; }
    public String getMensagem() { return mensagem; }
    public long getTimestamp() { return timestamp; }
    public ClienteUsuario getRemetente() { return remetente; }
}
