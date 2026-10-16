package tech.techparterns.identidade.application.command;

import java.util.UUID;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Operação sobre um convite.")
public record ConviteIdCommand(
        @Schema(description = "Id do convite (definido pela rota).") UUID id) {
}
