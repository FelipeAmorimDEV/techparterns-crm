package tech.techparterns.shared.infrastructure.rest;

import java.time.OffsetDateTime;

public record ErroResponse(
        String path,
        String mensagem,
        String codigoErro,
        OffsetDateTime dataHora) {
}
