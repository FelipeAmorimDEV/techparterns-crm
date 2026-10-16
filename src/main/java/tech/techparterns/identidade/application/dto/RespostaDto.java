package tech.techparterns.identidade.application.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Resposta neutra, idêntica para qualquer desfecho (não revela existência de conta).")
public record RespostaDto(String mensagem) {

    public static final RespostaDto SOLICITACAO_RECEBIDA = new RespostaDto("Solicitação recebida.");
}
