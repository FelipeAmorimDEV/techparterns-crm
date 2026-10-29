package tech.techparterns.identidade.infrastructure.security;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import tech.techparterns.shared.domain.exception.NaoAutenticadoException;
import tech.techparterns.shared.infrastructure.security.TenantContext;

/**
 * Lê usuário e empresa do token validando o formato (UUID). Token sem os dados vira 401, nunca 500.
 * A empresa vem SEMPRE do token, nunca de corpo ou parâmetro.
 */
@ApplicationScoped
public class ContextoAutenticado {

    private final TenantContext tenant;

    public ContextoAutenticado(TenantContext tenant) {
        this.tenant = tenant;
    }

    public UUID userId() {
        return uuid(tenant.userId());
    }

    public UUID empresaId() {
        return uuid(tenant.companyId());
    }

    public String papel() {
        return tenant.role();
    }

    private static UUID uuid(String valor) {
        try {
            if (valor == null || valor.isBlank())
                throw new IllegalArgumentException();
            return UUID.fromString(valor);
        } catch (IllegalArgumentException e) {
            throw new NaoAutenticadoException("Autenticação necessária");
        }
    }
}
