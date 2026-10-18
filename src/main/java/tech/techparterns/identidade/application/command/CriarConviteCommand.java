package tech.techparterns.identidade.application.command;

import java.util.UUID;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Criação de convite.")
public record CriarConviteCommand(
        @Schema(description = "E-mail do convidado (opcional).") String email,
        @Schema(description = "Validade (padrão: 7 dias).") java.time.Instant expiraEm,
        @Schema(description = "Limite de usos (padrão: 1).") Integer maxUsos) {
}
