package tech.techparterns.funil.application.dto;

import java.util.UUID;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "MotivoPerdaCriado", description = "Identificador do motivo de perda criado.")
public record MotivoPerdaCriadoDto(
        @Schema(description = "Identificador.")
        UUID id) {
}
