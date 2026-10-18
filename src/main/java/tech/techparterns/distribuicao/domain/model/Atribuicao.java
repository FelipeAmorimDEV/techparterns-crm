package tech.techparterns.distribuicao.domain.model;

import java.time.Instant;
import java.util.UUID;

public record Atribuicao(
        UUID id,
        String companyId,
        UUID leadId,
        UUID donoAnterior,
        UUID donoNovo,
        RegraAtribuicao regra,
        String motivo,
        UUID autorId,
        Instant ocorridoEm) {
}
