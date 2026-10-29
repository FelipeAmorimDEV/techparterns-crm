package tech.techparterns.funil.application.command;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "CriarEtapaRequest", description = "Dados para cadastrar uma etapa do funil.")
public record CriarEtapaCommand(
        @Schema(description = "Nome da etapa.", required = true)
        String nome,
        @Schema(description = "Tipo da etapa: ABERTO, GANHO ou PERDIDO.", required = true)
        String tipo,
        @Schema(description = "Ordem da etapa na empresa.", required = true)
        Integer ordem,
        @Schema(description = "Indica se a etapa fica ativa.", required = true)
        boolean ativa,
        @Schema(description = "Probabilidade de 0 a 100. Ausente quando não se aplica.")
        Integer probabilidade,
        @Schema(description = "Cor no formato # e seis dígitos hexadecimais.")
        String cor,
        @Schema(description = "Prazo em dias. Ausente quando não há SLA.")
        Integer slaDias,
        @Schema(description = "Exige roteiro de qualificação para entrar na etapa.", required = true)
        boolean exigeQualificacao,
        @Schema(description = "Exige data de demo para entrar na etapa.", required = true)
        boolean exigeDemo) {
}
