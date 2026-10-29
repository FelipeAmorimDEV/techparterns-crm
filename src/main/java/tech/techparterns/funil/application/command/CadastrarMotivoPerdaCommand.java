package tech.techparterns.funil.application.command;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "CadastrarMotivoPerdaRequest", description = "Dados para cadastrar um motivo de perda.")
public record CadastrarMotivoPerdaCommand(
        @Schema(description = "Nome do motivo de perda.", required = true)
        String nome,
        @Schema(description = "Indica se o motivo fica ativo.", required = true)
        Boolean ativo) {
}
