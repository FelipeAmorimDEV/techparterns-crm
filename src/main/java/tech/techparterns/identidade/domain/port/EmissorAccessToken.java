package tech.techparterns.identidade.domain.port;

import java.util.UUID;

import tech.techparterns.identidade.domain.model.PapelAcesso;

public interface EmissorAccessToken {

    int VALIDADE_SEGUNDOS = 600;

    /** {@code empresaId} é nulo para o dono da plataforma (sem claim companyId). */
    String emitir(UUID usuarioId, PapelAcesso papel, UUID empresaId);
}
