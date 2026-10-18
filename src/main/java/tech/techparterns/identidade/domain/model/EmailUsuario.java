package tech.techparterns.identidade.domain.model;

import java.util.Locale;
import java.util.regex.Pattern;

import tech.techparterns.shared.domain.Result;

/** E-mail normalizado: sem espaços nas pontas e em minúsculas. */
public final class EmailUsuario {

    private static final Pattern FORMATO = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final String valor;

    private EmailUsuario(String valor) { this.valor = valor; }

    public static Result<EmailUsuario> tryCreate(String valor) {
        String aparado = valor == null ? "" : valor.trim();
        if (aparado.isEmpty())
            return Result.fail(ErroIdentidade.EMAIL_OBRIGATORIO.mensagem());
        if (aparado.length() > 255)
            return Result.fail(ErroIdentidade.EMAIL_MUITO_LONGO.mensagem());
        if (!FORMATO.matcher(aparado).matches())
            return Result.fail(ErroIdentidade.EMAIL_INVALIDO.mensagem());
        return Result.ok(new EmailUsuario(aparado.toLowerCase(Locale.ROOT)));
    }

    public static EmailUsuario reconstituir(String valor) { return new EmailUsuario(valor); }

    public String valor() { return valor; }

    @Override
    public boolean equals(Object o) { return o instanceof EmailUsuario e && valor.equals(e.valor); }

    @Override
    public int hashCode() { return valor.hashCode(); }

    @Override
    public String toString() { return valor; }
}
