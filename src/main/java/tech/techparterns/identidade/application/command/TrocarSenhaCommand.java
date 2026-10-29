package tech.techparterns.identidade.application.command;

import java.util.UUID;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Troca da própria senha.")
public record TrocarSenhaCommand(
        @Schema(description = "Senha atual.", required = true) String senhaAtual,
        @Schema(description = "Nova senha.", required = true) String novaSenha) {
}
