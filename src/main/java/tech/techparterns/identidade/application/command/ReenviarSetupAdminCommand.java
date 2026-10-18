package tech.techparterns.identidade.application.command;

import java.util.UUID;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Reenvio do link de definição de senha do admin.")
public record ReenviarSetupAdminCommand(
        @Schema(description = "Id da empresa (definido pela rota).", hidden = true) UUID empresaId,
        @Schema(description = "Id do admin PENDING (opcional se houver só um).") UUID usuarioId) {
}
