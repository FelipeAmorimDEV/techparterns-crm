package tech.techparterns.identidade.domain.model;

import java.util.regex.Pattern;

import tech.techparterns.shared.domain.Result;

/** SHA-256 em hexadecimal (64 caracteres) de um token opaco. */
public final class HashToken {

    private static final Pattern HEX64 = Pattern.compile("^[0-9a-f]{64}$");

    private final String valor;

    private HashToken(String valor) { this.valor = valor; }

    public static Result<HashToken> tryCreate(String valor) {
        if (valor == null || !HEX64.matcher(valor).matches())
            return Result.fail(ErroIdentidade.HASH_TOKEN_INVALIDO.mensagem());
        return Result.ok(new HashToken(valor));
    }

    public static HashToken reconstituir(String valor) { return new HashToken(valor); }

    public String valor() { return valor; }

    @Override
    public boolean equals(Object o) { return o instanceof HashToken h && valor.equals(h.valor); }

    @Override
    public int hashCode() { return valor.hashCode(); }

    @Override
    public String toString() { return "HashToken[****]"; }
}
