package tech.techparterns.identidade.infrastructure.security;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.NewCookie;

import tech.techparterns.identidade.domain.model.RefreshToken;
import tech.techparterns.identidade.infrastructure.config.IdentidadeConfig;

/** Cookie do refresh: {@code HttpOnly; Secure; SameSite=Strict; Path=/auth/sessao} (DEL-09). */
@ApplicationScoped
public class CookieRefresh {

    public static final String NOME = "refresh_token";
    public static final String PATH = "/auth/sessao";

    private final IdentidadeConfig config;

    public CookieRefresh(IdentidadeConfig config) { this.config = config; }

    public NewCookie criar(String tokenPlano) {
        return construir(tokenPlano, (int) RefreshToken.VALIDADE.toSeconds());
    }

    public NewCookie limpar() {
        return construir("", 0);
    }

    private NewCookie construir(String valor, int maxAge) {
        return new NewCookie.Builder(NOME)
                .value(valor)
                .path(PATH)
                .httpOnly(true)
                .secure(config.cookie().secure())
                .sameSite(NewCookie.SameSite.STRICT)
                .maxAge(maxAge)
                .build();
    }
}
