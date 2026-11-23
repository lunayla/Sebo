package model;

import java.io.Serializable;

public class ClienteUsuario implements Serializable {
    private static final long serialVersionUID = 1L;
    private String nomeUsuario;
    private String perfil; // "CLIENTE" ou "ADMIN"

    public ClienteUsuario(String nomeUsuario, String perfil) {
        this.nomeUsuario = nomeUsuario;
        this.perfil = perfil;
    }

    public String getNomeUsuario() { return nomeUsuario; }
    public String getPerfil() { return perfil; }
}
