package tech.techparterns.identidade.application.command;

import java.util.UUID;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Aceite de convite.")
public record AceitarConviteCommand(
        @Schema(description = "Token do link.", required = true) String token,
        @Schema(description = "E-mail.", required = true) String email,
        @Schema(description = "Nome.", required = true) String nome,
        @Schema(description = "Telefone (E.164).") String telefone,
        @Schema(description = "Senha.", required = true) String senha) {
}
