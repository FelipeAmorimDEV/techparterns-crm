package tech.techparterns.shared.domain;

import tech.techparterns.shared.domain.exception.ErroDominio;

public final class FazendaId {

    private final String valor;

    private FazendaId(String valor) {
        this.valor = valor;
    }

    public static Result<FazendaId> tryCreate(String valor) {
        if (valor == null || valor.isBlank())
            return Result.fail(ErroDominio.FAZENDA_OBRIGATORIA.mensagem());
        return Result.ok(new FazendaId(valor.trim()));
    }

    public static FazendaId reconstituir(String valor) {
        return new FazendaId(valor);
    }

    public String valor() { return valor; }

    @Override
    public String toString() { return valor; }
}
