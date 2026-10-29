package tech.techparterns.funil.application.command;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "AtualizarDadosAbertosRequest",
        description = "Roteiro, demo e dados comerciais do lead aberto, sem mudar a etapa.")
public record AtualizarDadosAbertosCommand(
        @Schema(description = "Identificador do lead. Vem do caminho da rota.", hidden = true)
        UUID leadId,
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
        @Schema(description = "Valor estimado. Zero é aceito. Ausente quando não informado.")
        BigDecimal valorEstimado,
        @Schema(description = "Plano de interesse, até 40 caracteres. Ausente quando em branco.")
        String planoInteresse,
        @Schema(description = "Previsão de fechamento em yyyy-MM-dd. Vazio grava ausente.")
        String previsaoFechamento) {
}
