package tech.techparterns.lead.application.command;

import java.util.UUID;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Exclusão lógica de um lead da empresa do token.")
public record ExcluirLeadCommand(
        @Schema(description = "Identificador do lead.", required = true) UUID id) {
}
