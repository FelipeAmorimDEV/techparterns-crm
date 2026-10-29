package tech.techparterns.shared.domain;

import tech.techparterns.shared.domain.exception.ErroDominio;

public final class CompanyId {

    private final String valor;

    private CompanyId(String valor) {
        this.valor = valor;
    }

    public static Result<CompanyId> tryCreate(String valor) {
        if (valor == null || valor.isBlank())
            return Result.fail(ErroDominio.EMPRESA_OBRIGATORIA.mensagem());
        return Result.ok(new CompanyId(valor.trim()));
    }

    public static CompanyId reconstituir(String valor) {
        return new CompanyId(valor);
    }

    public String valor() { return valor; }

    @Override
    public String toString() { return valor; }
}
