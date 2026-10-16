package tech.techparterns.identidade.application.command;

import java.util.UUID;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Operação sobre uma empresa.")
public record EmpresaIdCommand(
        @Schema(description = "Id da empresa (definido pela rota).") UUID id) {
}
