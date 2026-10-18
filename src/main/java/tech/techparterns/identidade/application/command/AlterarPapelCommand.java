package tech.techparterns.identidade.application.command;

import java.util.UUID;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Alteração do papel de um usuário.")
public record AlterarPapelCommand(
        @Schema(description = "Id do usuário (definido pela rota).", hidden = true) UUID id,
        @Schema(description = "ADMIN ou SELLER.", required = true) String role) {
}
