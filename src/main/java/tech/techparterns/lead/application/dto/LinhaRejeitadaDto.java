package tech.techparterns.lead.application.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Linha de dados rejeitada na importação.")
public record LinhaRejeitadaDto(
        @Schema(description = "Número da linha no arquivo (cabeçalho = 1).") int linha,
        @Schema(description = "Motivos separados por '; '.") String motivo) {
}
