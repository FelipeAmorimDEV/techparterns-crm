package tech.techparterns.distribuicao.domain.model;

import java.time.Instant;
import java.util.UUID;

public record DonoLead(UUID leadId, String companyId, UUID ownerId, Instant atribuidoEm, boolean parado) {
}
