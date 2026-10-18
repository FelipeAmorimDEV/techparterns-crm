package tech.techparterns.identidade.infrastructure.security;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;

import tech.techparterns.identidade.domain.model.PapelAcesso;
import tech.techparterns.identidade.domain.port.EmissorAccessToken;

/** JWT de 10 min: {@code sub} UUID, {@code groups} com um único papel, {@code companyId} só para usuário de empresa. */
@ApplicationScoped
public class EmissorAccessTokenJwt implements EmissorAccessToken {

    private final String issuer;

    public EmissorAccessTokenJwt(@ConfigProperty(name = "mp.jwt.verify.issuer") String issuer) {
        this.issuer = issuer;
    }

    @Override
    public String emitir(UUID usuarioId, PapelAcesso papel, UUID empresaId) {
        var builder = Jwt.issuer(issuer)
                .subject(usuarioId.toString())
                .groups(Set.of(papel.name()))
                .expiresIn(Duration.ofSeconds(VALIDADE_SEGUNDOS));
        if (empresaId != null)
            builder.claim("companyId", empresaId.toString().toLowerCase());
        return builder.sign();
    }
}
