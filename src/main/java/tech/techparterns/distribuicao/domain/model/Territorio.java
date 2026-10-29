package tech.techparterns.distribuicao.domain.model;

import java.util.UUID;

public record Territorio(UUID id, String companyId, String uf, String cidade, UUID vendedorId) {
}
