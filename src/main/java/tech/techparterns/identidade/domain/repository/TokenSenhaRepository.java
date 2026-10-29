package tech.techparterns.identidade.domain.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import tech.techparterns.identidade.domain.model.HashToken;
import tech.techparterns.identidade.domain.model.TokenSenha;

public interface TokenSenhaRepository {

    void salvar(TokenSenha token);

    Optional<TokenSenha> buscarPorHash(HashToken hash);

    /** Invalida todos os tokens ainda pendentes do usuário. */
    void invalidarPendentesDoUsuario(UUID usuarioId, Instant agora);

    /** Consumo atômico: só afeta tokens não usados, não invalidados e não expirados. */
    boolean consumirSePossivel(UUID tokenId, Instant agora);
}
