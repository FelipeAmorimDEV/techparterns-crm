package tech.techparterns.shared.infrastructure.security;

import org.eclipse.microprofile.jwt.JsonWebToken;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

@RequestScoped
public class TenantContext {


    private final String userId;
    private final String companyId;
    private final String role;

    @Inject
    public TenantContext(JsonWebToken jwt, SecurityIdentity identity) {
        if (identity.isAnonymous()) {
            this.userId = null;
            this.companyId = null;
            this.role = null;
            return;
        }
        this.userId = jwt.getSubject();
        this.companyId = jwt.getClaim("companyId");
        this.role = identity.getRoles().stream().findFirst().orElse(null);
    }

    public String userId() {
        return userId;
    }

    public String companyId() {
        return companyId;
    }

    public String role() {
        return role;
    }

    public String requireCompanyId() {
        if (companyId == null || companyId.isBlank()) {
            throw new IllegalStateException("Company ausente no token");
        }
        return companyId;
    }

    public String requireUserId() {
        if (userId == null || userId.isBlank()) {
            throw new IllegalStateException("User ausente no token");
        }
        return userId;
    }
}
