package tech.techparterns.funil.application.command;

import java.util.UUID;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "MoverLeadEtapaRequest", description = "Etapa ABERTO de destino do lead.")
public record MoverLeadEtapaCommand(
        @Schema(description = "Identificador do lead. Vem do caminho da rota.", hidden = true)
        UUID leadId,
        @Schema(description = "Etapa de destino.", required = true)
        UUID etapaId) {
}
