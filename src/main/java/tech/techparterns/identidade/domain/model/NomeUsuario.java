package tech.techparterns.identidade.domain.model;

import tech.techparterns.shared.domain.Result;

public final class NomeUsuario {

    private final String valor;

    private NomeUsuario(String valor) { this.valor = valor; }

    public static Result<NomeUsuario> tryCreate(String valor) {
        String aparado = valor == null ? "" : valor.trim();
        if (aparado.isEmpty())
            return Result.fail(ErroIdentidade.NOME_OBRIGATORIO.mensagem());
        if (aparado.length() > 120)
            return Result.fail(ErroIdentidade.NOME_MUITO_LONGO.mensagem());
        return Result.ok(new NomeUsuario(aparado));
    }

    public static NomeUsuario reconstituir(String valor) { return new NomeUsuario(valor); }

    public String valor() { return valor; }

    @Override
    public boolean equals(Object o) { return o instanceof NomeUsuario e && valor.equals(e.valor); }

    @Override
    public int hashCode() { return valor.hashCode(); }

    @Override
    public String toString() { return valor; }
}
