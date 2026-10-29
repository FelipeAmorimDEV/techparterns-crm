package tech.techparterns.funil.application.command;

import java.math.BigDecimal;
import java.util.UUID;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "MarcarGanhoRequest", description = "Plano fechado e valor fechado do ganho.")
public record MarcarGanhoCommand(
        @Schema(description = "Identificador do lead. Vem do caminho da rota.", hidden = true)
        UUID leadId,
        @Schema(description = "Plano fechado, de 1 a 40 caracteres depois de tirar os espaços das pontas.",
                required = true)
        String planoFechado,
        @Schema(description = "Valor fechado, maior que zero.", required = true)
        BigDecimal valorFechado) {
}
