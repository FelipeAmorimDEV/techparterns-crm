package tech.techparterns.identidade.application.command;

import java.util.UUID;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Edição do próprio perfil (nome e telefone).")
public record EditarPerfilCommand(
        @Schema(description = "Novo nome.") String nome,
        @Schema(description = "Novo telefone (E.164).") String telefone,
        @Schema(description = "Não é permitido alterar o próprio e-mail; se informado e diferente, a requisição é recusada.") String email) {
}
