package tech.techparterns.lead.domain.model;

import java.util.Locale;

import tech.techparterns.shared.domain.Result;

public final class EmailContato {

    public static final String VAZIO = "E-mail vazio";

    private final String valor;

    private EmailContato(String valor) {
        this.valor = valor;
    }

    public static Result<EmailContato> tryCreate(String valor) {
        String aparado = valor == null ? "" : valor.trim();
        if (aparado.isEmpty())
            return Result.fail(VAZIO);
        return Result.ok(new EmailContato(aparado.toLowerCase(Locale.ROOT)));
    }

    public static EmailContato reconstituir(String valor) {
        return new EmailContato(valor);
    }

    public String valor() { return valor; }

    @Override
    public boolean equals(Object o) {
        return o instanceof EmailContato e && valor.equals(e.valor);
    }

    @Override
    public int hashCode() { return valor.hashCode(); }

    @Override
    public String toString() { return valor; }
}
