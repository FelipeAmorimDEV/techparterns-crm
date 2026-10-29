package tech.techparterns.identidade.application.command;

import java.util.UUID;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Login por e-mail e senha.")
public record LoginCommand(
        @Schema(description = "E-mail de acesso.", required = true) String email,
        @Schema(description = "Senha.", required = true) String senha) {
}
