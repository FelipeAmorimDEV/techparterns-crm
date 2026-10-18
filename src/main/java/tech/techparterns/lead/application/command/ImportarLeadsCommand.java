package tech.techparterns.lead.application.command;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Importação de leads por CSV UTF-8. A empresa vem do token.")
public record ImportarLeadsCommand(
        @Schema(description = "Bytes do arquivo CSV.", required = true) byte[] arquivo,
        @Schema(description = "Ação para duplicatas: ignorar, completar_campos_vazios ou criar_mesmo_assim.",
                required = true) String acao) {
}
