package tech.techparterns.identidade.application.service;

import java.util.UUID;

import tech.techparterns.identidade.domain.model.TipoTokenSenha;

/** Evento CDI disparado dentro da transação e observado só após o commit. O token plano vive apenas em memória. */
public record EmailParaEnviar(UUID envioId, String destinatario, String nomeDestinatario, String nomeEmpresa,
                              TipoTokenSenha tipo, String tokenPlano) {

    @Override
    public String toString() {
        return "EmailParaEnviar[envioId=" + envioId + ", tipo=" + tipo + "]";
    }
}
