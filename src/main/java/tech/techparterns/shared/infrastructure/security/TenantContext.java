package tech.techparterns.shared.infrastructure.security;

import org.eclipse.microprofile.jwt.JsonWebToken;

import tech.techparterns.shared.domain.exception.CulturaObrigatoriaException;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

@RequestScoped
public class TenantContext {

    public static final String HEADER_CULTURA = "X-Cultura-Id";

    private final String userId;
    private final String fazendaId;
    private final String role;
    private String culturaId;

    @Inject
    public TenantContext(JsonWebToken jwt, SecurityIdentity identity) {
        if (identity.isAnonymous()) {
            this.userId = null;
            this.fazendaId = null;
            this.role = null;
            return;
        }
        this.userId = jwt.getSubject();
        this.fazendaId = jwt.getClaim("fazendaId");
        this.role = identity.getRoles().stream().findFirst().orElse(null);
    }

    public String userId() {
        return userId;
    }

    public String fazendaId() {
        return fazendaId;
    }

    public String role() {
        return role;
    }

    public String requireFazendaId() {
        if (fazendaId == null || fazendaId.isBlank()) {
            throw new IllegalStateException("Tenant ausente no token");
        }
        return fazendaId;
    }

    public String requireUserId() {
        if (userId == null || userId.isBlank()) {
            throw new IllegalStateException("Usuário ausente no token");
        }
        return userId;
    }

    public void bindCultura(String culturaId) {
        this.culturaId = culturaId;
    }

    public String requireCulturaId() {
        if (culturaId == null || culturaId.isBlank()) {
            throw new CulturaObrigatoriaException();
        }
        return culturaId;
    }
}
