package tech.techparterns.identidade.application.command;

import java.util.UUID;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Pedido de recuperação de senha.")
public record SolicitarRecuperacaoSenhaCommand(
        @Schema(description = "E-mail da conta.", required = true) String email) {
}
