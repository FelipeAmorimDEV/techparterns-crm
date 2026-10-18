package tech.techparterns.identidade.application.usecase;

import tech.techparterns.identidade.domain.model.ErroIdentidade;
import tech.techparterns.identidade.domain.model.PapelUsuario;
import tech.techparterns.shared.domain.Result;

/** Converte texto em {@link PapelUsuario}; valor ausente assume o padrão, valor fora de ADMIN e SELLER é erro. */
final class PapelParser {

    private PapelParser() {}

    static Result<PapelUsuario> parse(String valor, PapelUsuario padrao) {
        if (valor == null || valor.isBlank())
            return padrao != null ? Result.ok(padrao) : Result.fail(ErroIdentidade.PAPEL_INVALIDO.mensagem());
        try {
            return Result.ok(PapelUsuario.valueOf(valor.trim().toUpperCase(java.util.Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            return Result.fail(ErroIdentidade.PAPEL_INVALIDO.mensagem());
        }
    }
}
