package tech.techparterns.identidade.application.command;

import java.util.UUID;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Renovação de sessão.")
public record RenovarSessaoCommand(
        @Schema(description = "Refresh token (vem do cookie).") String refreshToken) {
}
