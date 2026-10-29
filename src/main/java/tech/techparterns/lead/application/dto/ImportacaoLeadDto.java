package tech.techparterns.lead.application.dto;

import java.util.List;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Resultado síncrono da importação de leads.")
public record ImportacaoLeadDto(
        @Schema(description = "Leads criados sem duplicata.") int criados,
        @Schema(description = "Linhas ignoradas por duplicidade.") int ignorados,
        @Schema(description = "Duplicatas tratadas por completar campos vazios.") int completados,
        @Schema(description = "Leads criados mesmo com duplicata.") int criadosMesmoAssim,
        @Schema(description = "Linhas rejeitadas.") int rejeitados,
        @Schema(description = "Linhas rejeitadas com motivo.") List<LinhaRejeitadaDto> rejeitadas) {
}
