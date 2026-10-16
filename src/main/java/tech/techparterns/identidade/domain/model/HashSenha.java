package tech.techparterns.identidade.domain.model;

public final class HashSenha {

    private final String valor;

    private HashSenha(String valor) { this.valor = valor; }

    public static HashSenha of(String valor) {
        if (valor == null || valor.isBlank())
            throw new IllegalArgumentException("Hash de senha vazio");
        return new HashSenha(valor);
    }

    public static HashSenha reconstituir(String valor) { return new HashSenha(valor); }

    public String valor() { return valor; }

    @Override
    public String toString() { return "HashSenha[****]"; }
}
