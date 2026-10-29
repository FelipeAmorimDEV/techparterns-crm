package tech.techparterns.funil.application.command;

import java.util.UUID;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "AlterarEtapaRequest", description = "Nome, ordem, probabilidade, cor e prazo da etapa.")
public record AlterarEtapaCommand(
        @Schema(description = "Identificador da etapa. Vem do caminho da rota.", hidden = true)
        UUID id,
        @Schema(description = "Nome da etapa.", required = true)
        String nome,
        @Schema(description = "Ordem da etapa na empresa.", required = true)
        Integer ordem,
        @Schema(description = "Probabilidade de 0 a 100. Ausente quando não se aplica.")
        Integer probabilidade,
        @Schema(description = "Cor no formato # e seis dígitos hexadecimais.")
        String cor,
        @Schema(description = "Prazo em dias. Ausente quando não há SLA.")
        Integer slaDias) {
}
