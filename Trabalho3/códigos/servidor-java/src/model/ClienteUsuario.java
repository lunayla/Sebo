package model;

public class ClienteUsuario {
    private String nomeUsuario;
    private String papel;

    public ClienteUsuario(String nomeUsuario, String papel) {
        this.nomeUsuario = nomeUsuario;
        this.papel = papel;
    }

    public String getNomeUsuario() { return nomeUsuario; }
    public String getPapel() { return papel; }
}
