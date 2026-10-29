package tech.techparterns.funil.application.dto;

import java.util.UUID;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "MotivoPerdaItem", description = "Motivo de perda da empresa autenticada.")
public record MotivoPerdaItemDto(
        @Schema(description = "Identificador.")
        UUID id,
        @Schema(description = "Nome do motivo de perda.")
        String nome,
        @Schema(description = "Indica se o motivo está ativo.")
        boolean ativo) {
}
