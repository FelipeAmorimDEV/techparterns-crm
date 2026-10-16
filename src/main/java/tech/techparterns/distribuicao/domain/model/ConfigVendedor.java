package tech.techparterns.distribuicao.domain.model;

import java.util.UUID;

public record ConfigVendedor(String companyId, UUID userId, int peso, Integer maxLeadsAbertos, boolean participa) {

    public static ConfigVendedor padrao(String companyId, UUID userId) {
        return new ConfigVendedor(companyId, userId, 1, null, true);
    }
}
