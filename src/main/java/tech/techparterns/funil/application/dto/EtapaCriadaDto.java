package tech.techparterns.funil.application.dto;

import java.util.UUID;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "EtapaCriada", description = "Identificador da etapa criada.")
public record EtapaCriadaDto(
        @Schema(description = "Identificador.")
        UUID id) {
}
