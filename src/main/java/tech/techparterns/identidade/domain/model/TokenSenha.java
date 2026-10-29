package tech.techparterns.identidade.domain.model;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

import tech.techparterns.shared.domain.Entity;

/** Token de definição (SETUP, 48 h) ou de recuperação (RESET, 1 h) de senha. Uso único. */
public class TokenSenha extends Entity {

    public static final Duration VALIDADE_SETUP = Duration.ofHours(48);
    public static final Duration VALIDADE_RESET = Duration.ofHours(1);

    private final UUID usuarioId;
    private final TipoTokenSenha tipo;
    private final HashToken hashToken;
    private final Instant expiraEm;
    private Instant usadoEm;
    private Instant invalidadoEm;

    private TokenSenha(UUID id, Instant createdAt, Instant updatedAt, OffsetDateTime deletedAt, UUID usuarioId,
                       TipoTokenSenha tipo, HashToken hashToken, Instant expiraEm, Instant usadoEm,
                       Instant invalidadoEm) {
        super(id, createdAt, updatedAt, deletedAt);
        this.usuarioId = usuarioId;
        this.tipo = tipo;
        this.hashToken = hashToken;
        this.expiraEm = expiraEm;
        this.usadoEm = usadoEm;
        this.invalidadoEm = invalidadoEm;
    }

    public static TokenSenha criar(UUID usuarioId, TipoTokenSenha tipo, HashToken hash, Instant agora) {
        Duration validade = tipo == TipoTokenSenha.SETUP ? VALIDADE_SETUP : VALIDADE_RESET;
        return new TokenSenha(UUID.randomUUID(), agora, agora, null, usuarioId, tipo, hash,
                agora.plus(validade), null, null);
    }

    public static TokenSenha reconstituir(UUID id, Instant createdAt, Instant updatedAt, OffsetDateTime deletedAt,
                                          UUID usuarioId, TipoTokenSenha tipo, HashToken hashToken,
                                          Instant expiraEm, Instant usadoEm, Instant invalidadoEm) {
        return new TokenSenha(id, createdAt, updatedAt, deletedAt, usuarioId, tipo, hashToken, expiraEm,
                usadoEm, invalidadoEm);
    }

    /** Válido enquanto não usado, não invalidado e antes do instante de expiração. */
    public boolean estaValido(Instant agora) {
        return usadoEm == null && invalidadoEm == null && agora.isBefore(expiraEm);
    }

    public void consumir(Instant agora) { this.usadoEm = agora; }

    public void invalidar(Instant agora) {
        if (invalidadoEm == null)
            invalidadoEm = agora;
    }

    public UUID usuarioId() { return usuarioId; }
    public TipoTokenSenha tipo() { return tipo; }
    public HashToken hashToken() { return hashToken; }
    public Instant expiraEm() { return expiraEm; }
    public Instant usadoEm() { return usadoEm; }
    public Instant invalidadoEm() { return invalidadoEm; }
}
