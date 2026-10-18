package tech.techparterns.identidade.infrastructure.rest;

import tech.techparterns.shared.domain.Result;
import tech.techparterns.shared.domain.exception.DomainException;

/** Converte um {@code Result} com falha em {@code DomainException} (400 pelo handler global). */
final class Respostas {

    private Respostas() {}

    static <T> T valor(Result<T> resultado) {
        if (resultado.isFailure())
            throw new DomainException(resultado.getErrors());
        return resultado.getValue();
    }
}
