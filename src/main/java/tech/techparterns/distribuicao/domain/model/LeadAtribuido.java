package tech.techparterns.distribuicao.domain.model;

import java.time.Instant;
import java.util.UUID;

public record LeadAtribuido(
        String companyId,
        UUID leadId,
        UUID donoAnterior,
        UUID donoNovo,
        String regra,
        UUID atribuicaoId) {
}
