package tech.techparterns.funil.application.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "QuadroFunil", description = "Quadro do funil da empresa autenticada, sem página.")
public record QuadroFunilDto(
        @Schema(description = "Colunas das etapas da empresa.")
        List<Coluna> colunas) {

    @Schema(name = "ColunaQuadro", description = "Coluna de uma etapa, com a quantidade e o valor dos leads visíveis.")
    public record Coluna(
            @Schema(description = "Identificador da etapa.")
            UUID etapaId,
            @Schema(description = "Tipo da etapa: ABERTO, GANHO ou PERDIDO.")
            String tipo,
            @Schema(description = "Quantidade de leads visíveis na coluna.")
            int quantidade,
            @Schema(description = "Valor da coluna para os leads visíveis.")
            BigDecimal valor,
            @Schema(description = "Leads visíveis nesta coluna.")
            List<Item> itens) {
    }

    @Schema(name = "ItemQuadro", description = "Lead visível no quadro.")
    public record Item(
            @Schema(description = "Identificador do lead.")
            UUID leadId,
            @Schema(description = "Identificador da etapa.")
            UUID etapaId,
            @Schema(description = "Atraso na etapa atual.")
            boolean atrasado) {
    }
}
