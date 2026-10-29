package tech.techparterns.funil.application.command;

import java.util.UUID;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Exclusão lógica de uma etapa da empresa do token.")
public record ApagarEtapaCommand(
        @Schema(description = "Identificador da etapa.", required = true)
        UUID id) {
}
