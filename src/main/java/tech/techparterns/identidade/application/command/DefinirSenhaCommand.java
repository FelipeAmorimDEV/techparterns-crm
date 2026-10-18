package tech.techparterns.identidade.application.command;

import java.util.UUID;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Definição de senha por token.")
public record DefinirSenhaCommand(
        @Schema(description = "Token do link (SETUP ou RESET).", required = true) String token,
        @Schema(description = "Nova senha.", required = true) String senha) {
}
