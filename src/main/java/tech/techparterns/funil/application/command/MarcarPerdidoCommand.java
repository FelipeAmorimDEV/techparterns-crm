package tech.techparterns.funil.application.command;

import java.util.UUID;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "MarcarPerdidoRequest", description = "Motivo de perda ativo e observação opcional.")
public record MarcarPerdidoCommand(
        @Schema(description = "Identificador do lead. Vem do caminho da rota.", hidden = true)
        UUID leadId,
        @Schema(description = "Motivo de perda ativo da empresa.", required = true)
        UUID motivoPerdaId,
        @Schema(description = "Observação. Pode ser omitida.")
        String observacao) {
}
