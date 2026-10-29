package tech.techparterns.identidade.suporte;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

import io.smallrye.jwt.build.Jwt;

/** Emite JWT de teste com as chaves descartáveis de src/test/resources/chaves. */
public final class TokenTeste {

    public static final String ISSUER = "techparterns-teste";

    private TokenTeste() {}

    public static String admin(UUID userId, UUID empresaId) {
        return emitir(userId, "ADMIN", empresaId);
    }

    public static String vendedor(UUID userId, UUID empresaId) {
        return emitir(userId, "SELLER", empresaId);
    }

    public static String dono(UUID userId) {
        return emitir(userId, "PLATFORM_OWNER", null);
    }

    public static String emitir(UUID userId, String papel, UUID empresaId) {
        var b = Jwt.issuer(ISSUER).subject(userId.toString()).groups(Set.of(papel)).expiresIn(Duration.ofMinutes(10));
        if (empresaId != null)
            b.claim("companyId", empresaId.toString());
        return b.sign();
    }
}
