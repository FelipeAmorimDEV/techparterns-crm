package tech.techparterns.identidade.application.command;

import java.util.UUID;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Edição do nome da empresa.")
public record EditarEmpresaCommand(
        @Schema(description = "Id da empresa (definido pela rota).", hidden = true) UUID id,
        @Schema(description = "Novo nome.", required = true) String nome) {
}
