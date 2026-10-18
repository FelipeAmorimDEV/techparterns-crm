package tech.techparterns.identidade.domain.model;

import tech.techparterns.shared.domain.Result;

public final class NomeEmpresa {

    private final String valor;

    private NomeEmpresa(String valor) { this.valor = valor; }

    public static Result<NomeEmpresa> tryCreate(String valor) {
        String aparado = valor == null ? "" : valor.trim();
        if (aparado.isEmpty())
            return Result.fail(ErroIdentidade.EMPRESA_NOME_OBRIGATORIO.mensagem());
        if (aparado.length() > 120)
            return Result.fail(ErroIdentidade.EMPRESA_NOME_MUITO_LONGO.mensagem());
        return Result.ok(new NomeEmpresa(aparado));
    }

    public static NomeEmpresa reconstituir(String valor) { return new NomeEmpresa(valor); }

    public String valor() { return valor; }

    @Override
    public boolean equals(Object o) { return o instanceof NomeEmpresa e && valor.equals(e.valor); }

    @Override
    public int hashCode() { return valor.hashCode(); }

    @Override
    public String toString() { return valor; }
}
