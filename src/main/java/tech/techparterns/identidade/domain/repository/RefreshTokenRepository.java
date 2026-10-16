package tech.techparterns.identidade.domain.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import tech.techparterns.identidade.domain.model.HashToken;
import tech.techparterns.identidade.domain.model.RefreshToken;
import tech.techparterns.identidade.domain.model.TitularTipo;

public interface RefreshTokenRepository {

    void salvar(RefreshToken token);

    Optional<RefreshToken> buscarPorHash(HashToken hash);

    /** {@code UPDATE ... WHERE rotacionado_em IS NULL AND revogado_em IS NULL}: um só vencedor. */
    boolean rotacionarSePossivel(UUID tokenId, Instant agora);

    void revogarFamilia(UUID familiaId, Instant agora);

    void revogarTodasDoTitular(TitularTipo tipo, UUID titularId, Instant agora);
}
