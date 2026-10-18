package tech.techparterns.identidade.application.command;

import java.util.UUID;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Suspensão de empresa.")
public record SuspenderEmpresaCommand(
        @Schema(description = "Id da empresa (definido pela rota).", hidden = true) UUID id,
        @Schema(description = "Motivo (até 255 caracteres).") String motivo) {
}
