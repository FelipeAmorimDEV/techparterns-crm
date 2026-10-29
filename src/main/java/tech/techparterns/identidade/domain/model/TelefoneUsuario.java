package tech.techparterns.identidade.domain.model;

import java.util.regex.Pattern;

import tech.techparterns.shared.domain.Result;

/**
 * Telefone em formato E.164 ({@code +} e 8 a 15 dígitos, máximo 20 caracteres).
 * Regra provisória (Q-16), isolada neste VO para troca futura.
 */
public final class TelefoneUsuario {

    private static final Pattern E164 = Pattern.compile("^\\+[0-9]{8,15}$");

    private final String valor;

    private TelefoneUsuario(String valor) { this.valor = valor; }

    public static Result<TelefoneUsuario> tryCreate(String valor) {
        String aparado = valor == null ? "" : valor.trim();
        if (aparado.length() > 20 || !E164.matcher(aparado).matches())
            return Result.fail(ErroIdentidade.TELEFONE_INVALIDO.mensagem());
        return Result.ok(new TelefoneUsuario(aparado));
    }

    /** Telefone opcional: nulo ou em branco resulta em {@code Result.ok(null)}. */
    public static Result<TelefoneUsuario> tryCreateOpcional(String valor) {
        if (valor == null || valor.isBlank())
            return Result.ok(null);
        return tryCreate(valor);
    }

    public static TelefoneUsuario reconstituir(String valor) { return new TelefoneUsuario(valor); }

    public String valor() { return valor; }

    @Override
    public boolean equals(Object o) { return o instanceof TelefoneUsuario t && valor.equals(t.valor); }

    @Override
    public int hashCode() { return valor.hashCode(); }

    @Override
    public String toString() { return valor; }
}
