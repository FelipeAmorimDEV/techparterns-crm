package tech.techparterns.lead.domain.model;

import tech.techparterns.shared.domain.Result;

public final class NomeContato {

    public static final int MAX = 120;
    public static final String VAZIO = "Nome do contato vazio";
    public static final String LONGO = "Nome do contato acima de 120 caracteres";

    private final String valor;

    private NomeContato(String valor) {
        this.valor = valor;
    }

    public static Result<NomeContato> tryCreate(String valor) {
        String aparado = valor == null ? "" : valor.trim();
        if (aparado.isEmpty())
            return Result.fail(VAZIO);
        if (aparado.length() > MAX)
            return Result.fail(LONGO);
        return Result.ok(new NomeContato(aparado));
    }

    public static NomeContato reconstituir(String valor) {
        return new NomeContato(valor);
    }

    public String valor() { return valor; }

    @Override
    public boolean equals(Object o) {
        return o instanceof NomeContato n && valor.equals(n.valor);
    }

    @Override
    public int hashCode() { return valor.hashCode(); }

    @Override
    public String toString() { return valor; }
}
