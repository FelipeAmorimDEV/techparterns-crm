package tech.techparterns.identidade.domain.model;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

import tech.techparterns.shared.domain.Entity;

/** Refresh token opaco (7 dias). Uma família de tokens é uma sessão. */
public class RefreshToken extends Entity {

    public static final Duration VALIDADE = Duration.ofDays(7);

    private final TitularTipo titularTipo;
    private final UUID titularId;
    private final UUID familiaId;
    private final HashToken hashToken;
    private final Instant expiraEm;
    private Instant rotacionadoEm;
    private Instant revogadoEm;

    private RefreshToken(UUID id, Instant createdAt, Instant updatedAt, OffsetDateTime deletedAt,
                         TitularTipo titularTipo, UUID titularId, UUID familiaId, HashToken hashToken,
                         Instant expiraEm, Instant rotacionadoEm, Instant revogadoEm) {
        super(id, createdAt, updatedAt, deletedAt);
        this.titularTipo = titularTipo;
        this.titularId = titularId;
        this.familiaId = familiaId;
        this.hashToken = hashToken;
        this.expiraEm = expiraEm;
        this.rotacionadoEm = rotacionadoEm;
        this.revogadoEm = revogadoEm;
    }

    /** {@code familiaId} nulo abre uma nova família (login). */
    public static RefreshToken criar(TitularTipo tipo, UUID titularId, UUID familiaId, HashToken hash, Instant agora) {
        return new RefreshToken(UUID.randomUUID(), agora, agora, null, tipo, titularId,
                familiaId != null ? familiaId : UUID.randomUUID(), hash, agora.plus(VALIDADE), null, null);
    }

    public static RefreshToken reconstituir(UUID id, Instant createdAt, Instant updatedAt, OffsetDateTime deletedAt,
                                            TitularTipo titularTipo, UUID titularId, UUID familiaId,
                                            HashToken hashToken, Instant expiraEm, Instant rotacionadoEm,
                                            Instant revogadoEm) {
        return new RefreshToken(id, createdAt, updatedAt, deletedAt, titularTipo, titularId, familiaId, hashToken,
                expiraEm, rotacionadoEm, revogadoEm);
    }

    public boolean expirado(Instant agora) { return !agora.isBefore(expiraEm); }
    public boolean foiRotacionado() { return rotacionadoEm != null; }
    public boolean foiRevogado() { return revogadoEm != null; }

    public TitularTipo titularTipo() { return titularTipo; }
    public UUID titularId() { return titularId; }
    public UUID familiaId() { return familiaId; }
    public HashToken hashToken() { return hashToken; }
    public Instant expiraEm() { return expiraEm; }
    public Instant rotacionadoEm() { return rotacionadoEm; }
    public Instant revogadoEm() { return revogadoEm; }
}
