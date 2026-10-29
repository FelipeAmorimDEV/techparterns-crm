package tech.techparterns.identidade.application.command;

import java.util.UUID;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Operação sobre um usuário da empresa.")
public record UsuarioIdCommand(
        @Schema(description = "Id do usuário (definido pela rota).") UUID id) {
}
