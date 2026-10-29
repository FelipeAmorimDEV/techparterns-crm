package tech.techparterns.identidade.application.command;

import java.util.UUID;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Edição de usuário da empresa.")
public record EditarUsuarioCommand(
        @Schema(description = "Id do usuário (definido pela rota).", hidden = true) UUID id,
        @Schema(description = "Novo nome.") String nome,
        @Schema(description = "Novo e-mail (não vale para o próprio admin).") String email,
        @Schema(description = "Novo telefone.") String telefone) {
}
