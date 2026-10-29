package tech.techparterns.funil.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "DadosAbertos", description = "Roteiro, demo e dados comerciais gravados, com a etapa atual.")
public record DadosAbertosDto(
        @Schema(description = "Etapa em que o lead permanece.")
        UUID etapaId,
        @Schema(description = "Há decisor. Em branco quando ausente.")
        Boolean decisor,
        @Schema(description = "Há dor principal. Em branco quando ausente.")
        Boolean dorPrincipal,
        @Schema(description = "Há usuários previstos. Em branco quando ausente.")
        Boolean usuariosPrevistos,
        @Schema(description = "Há prazo de decisão. Em branco quando ausente.")
        Boolean prazoDecisao,
        @Schema(description = "Há orçamento. Em branco quando ausente.")
        Boolean orcamento,
        @Schema(description = "Data da demo. Ausente quando não houve demo.")
        LocalDate dataDemo,
        @Schema(description = "Valor estimado. Ausente quando não informado.")
        BigDecimal valorEstimado,
        @Schema(description = "Plano de interesse. Ausente quando não informado.")
        String planoInteresse,
        @Schema(description = "Previsão de fechamento. Ausente quando não informada.")
        LocalDate previsaoFechamento) {
}
