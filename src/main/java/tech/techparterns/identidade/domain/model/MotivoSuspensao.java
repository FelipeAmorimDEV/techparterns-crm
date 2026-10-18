package tech.techparterns.identidade.domain.model;

import tech.techparterns.shared.domain.Result;

public final class MotivoSuspensao {

    private final String valor;

    private MotivoSuspensao(String valor) { this.valor = valor; }

    /** Motivo opcional: nulo ou em branco resulta em {@code Result.ok(null)}. */
    public static Result<MotivoSuspensao> tryCreate(String valor) {
        if (valor == null || valor.isBlank())
            return Result.ok(null);
        String aparado = valor.trim();
        if (aparado.length() > 255)
            return Result.fail(ErroIdentidade.MOTIVO_MUITO_LONGO.mensagem());
        return Result.ok(new MotivoSuspensao(aparado));
    }

    public static MotivoSuspensao reconstituir(String valor) { return new MotivoSuspensao(valor); }

    public String valor() { return valor; }

    @Override
    public String toString() { return valor; }
}
