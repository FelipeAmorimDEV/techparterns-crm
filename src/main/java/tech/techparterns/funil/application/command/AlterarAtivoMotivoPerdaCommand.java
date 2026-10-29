package tech.techparterns.funil.application.command;

import java.util.UUID;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "AlterarAtivoMotivoPerdaRequest", description = "Novo indicador de ativo do motivo de perda.")
public record AlterarAtivoMotivoPerdaCommand(
        @Schema(description = "Identificador do motivo. Vem do caminho da rota.", hidden = true)
        UUID id,
        @Schema(description = "Indica se o motivo fica ativo.", required = true)
        Boolean ativo) {
}
