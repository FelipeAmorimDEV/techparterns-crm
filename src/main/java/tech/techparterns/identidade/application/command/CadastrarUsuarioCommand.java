package tech.techparterns.identidade.application.command;

import java.util.UUID;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Cadastro de usuário da própria empresa.")
public record CadastrarUsuarioCommand(
        @Schema(description = "Nome.", required = true) String nome,
        @Schema(description = "E-mail.", required = true) String email,
        @Schema(description = "Telefone (E.164).") String telefone,
        @Schema(description = "ADMIN ou SELLER (padrão SELLER).") String role) {
}
