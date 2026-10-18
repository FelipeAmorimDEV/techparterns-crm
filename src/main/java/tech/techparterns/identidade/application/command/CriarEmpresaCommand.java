package tech.techparterns.identidade.application.command;

import java.util.UUID;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Cadastro de empresa com o primeiro admin.")
public record CriarEmpresaCommand(
        @Schema(description = "Nome da empresa.", required = true) String nome,
        @Schema(description = "Nome do primeiro admin.", required = true) String adminNome,
        @Schema(description = "E-mail do primeiro admin.", required = true) String adminEmail,
        @Schema(description = "Telefone do primeiro admin.") String adminTelefone) {
}
