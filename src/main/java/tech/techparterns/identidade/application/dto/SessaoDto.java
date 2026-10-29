package tech.techparterns.identidade.application.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Sessão: access token JWT (o refresh vai em cookie HttpOnly).")
public record SessaoDto(
        @Schema(description = "Access token JWT.") String accessToken,
        @Schema(description = "Tipo do token.", example = "Bearer") String tipo,
        @Schema(description = "Validade em segundos.", example = "600") long expiraEmSegundos) {

    public static SessaoDto de(String accessToken, long expiraEmSegundos) {
        return new SessaoDto(accessToken, "Bearer", expiraEmSegundos);
    }
}
