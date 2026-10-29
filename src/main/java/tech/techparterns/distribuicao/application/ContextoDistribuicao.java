package tech.techparterns.distribuicao.application;

import java.util.UUID;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.ApplicationScoped;

import tech.techparterns.distribuicao.domain.port.VendedoresDistribuicao;
import tech.techparterns.shared.infrastructure.security.TenantContext;

@ApplicationScoped
public class ContextoDistribuicao {

    private final TenantContext tenant;
    private final SecurityIdentity identity;
    private final VendedoresDistribuicao vendedores;

    public ContextoDistribuicao(TenantContext tenant, SecurityIdentity identity, VendedoresDistribuicao vendedores) {
        this.tenant = tenant;
        this.identity = identity;
        this.vendedores = vendedores;
    }

    public String empresa() {
        return tenant.requireCompanyId();
    }

    public UUID usuario() {
        var id = tenant.userId();
        if (id == null || id.isBlank()) {
            var principal = identity.getPrincipal();
            id = principal == null ? null : principal.getName();
        }
        if (id == null || id.isBlank())
            throw new IllegalStateException("User ausente no token");
        return UUID.fromString(id);
    }

    public boolean admin() {
        return "ADMIN".equals(tenant.role());
    }

    public VendedoresDistribuicao.Cadastro vendedorDaEmpresa(UUID id) {
        return vendedores.buscar(empresa(), id).orElse(null);
    }
}
