package tech.techparterns.identidade.application.command;

import java.util.UUID;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Logout.")
public record EncerrarSessaoCommand(
        @Schema(description = "Refresh token (vem do cookie).") String refreshToken) {
}
