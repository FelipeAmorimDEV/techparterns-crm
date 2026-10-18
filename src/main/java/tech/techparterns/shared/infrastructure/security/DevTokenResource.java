package tech.techparterns.shared.infrastructure.security;

import java.time.Duration;
import java.util.Set;

import io.quarkus.arc.profile.IfBuildProfile;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

/** Emite tokens JWT para testes manuais. Existe somente no perfil dev. */
@IfBuildProfile("dev")
@Path("/dev/token")
@ApplicationScoped
public class DevTokenResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String emitir(
            @QueryParam("companyId") String companyId,
            @QueryParam("userId") String userId,
            @QueryParam("role") String role) {
        return Jwt.issuer("techparterns-dev")
                .subject(userId == null ? "00000000-0000-0000-0000-000000000001" : userId)
                .claim("companyId", companyId == null ? "00000000-0000-0000-0000-0000000000c1" : companyId)
                .groups(Set.of(role == null ? "admin" : role))
                .expiresIn(Duration.ofHours(8))
                .sign();
    }
}
