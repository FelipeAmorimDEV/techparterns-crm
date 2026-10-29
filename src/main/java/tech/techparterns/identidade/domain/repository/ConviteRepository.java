package tech.techparterns.identidade.domain.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import tech.techparterns.identidade.domain.model.Convite;
import tech.techparterns.identidade.domain.model.HashToken;

public interface ConviteRepository {

    void salvar(Convite convite);

    Optional<Convite> buscarPorHash(HashToken hash);

    /** Filtra id e empresa (404 para outra empresa). */
    Optional<Convite> buscarPorId(UUID id, UUID empresaId);

    List<Convite> listar(UUID empresaId);

    /**
     * Incremento atômico: {@code usos = usos + 1 WHERE usos < max_usos AND não revogado AND não expirado}.
     * @return {@code true} se uma linha foi atualizada
     */
    boolean incrementarUsoSePossivel(UUID conviteId, Instant agora);
}
