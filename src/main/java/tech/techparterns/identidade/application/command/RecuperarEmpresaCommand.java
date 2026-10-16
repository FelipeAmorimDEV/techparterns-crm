package tech.techparterns.identidade.application.command;

import java.util.UUID;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Recuperação de empresa sem admin ativo.")
public record RecuperarEmpresaCommand(
        @Schema(description = "Id da empresa (definido pela rota).", hidden = true) UUID empresaId,
        @Schema(description = "Nome do novo admin.", required = true) String adminNome,
        @Schema(description = "E-mail do novo admin.", required = true) String adminEmail,
        @Schema(description = "Telefone do novo admin.") String adminTelefone) {
}
