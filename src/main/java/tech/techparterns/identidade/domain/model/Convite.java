package tech.techparterns.identidade.domain.model;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

import tech.techparterns.shared.domain.Entity;
import tech.techparterns.shared.domain.Result;

public class Convite extends Entity {

    public static final Duration VALIDADE_PADRAO = Duration.ofDays(7);
    public static final int MAX_USOS_PADRAO = 1;

    private final UUID empresaId;
    private final EmailUsuario email;
    private final HashToken hashToken;
    private final Instant expiraEm;
    private final int maxUsos;
    private int usos;
    private Instant revogadoEm;
    private final UUID criadoPor;

    private Convite(UUID id, Instant createdAt, Instant updatedAt, OffsetDateTime deletedAt, UUID empresaId,
                    EmailUsuario email, HashToken hashToken, Instant expiraEm, int maxUsos, int usos,
                    Instant revogadoEm, UUID criadoPor) {
        super(id, createdAt, updatedAt, deletedAt);
        this.empresaId = empresaId;
        this.email = email;
        this.hashToken = hashToken;
        this.expiraEm = expiraEm;
        this.maxUsos = maxUsos;
        this.usos = usos;
        this.revogadoEm = revogadoEm;
        this.criadoPor = criadoPor;
    }

    /** {@code expiraEm} e {@code maxUsos} nulos assumem os padrões (7 dias e 1 uso). */
    public static Result<Convite> tryCreate(UUID empresaId, EmailUsuario emailOpcional, HashToken hash,
                                            Instant expiraEm, Integer maxUsos, UUID criadoPor, Instant agora) {
        Instant validade = expiraEm != null ? expiraEm : agora.plus(VALIDADE_PADRAO);
        int limite = maxUsos != null ? maxUsos : MAX_USOS_PADRAO;
        if (!validade.isAfter(agora))
            return Result.fail(ErroIdentidade.EXPIRACAO_INVALIDA.mensagem());
        if (limite < 1)
            return Result.fail(ErroIdentidade.USOS_INVALIDOS.mensagem());
        if (empresaId == null || hash == null || criadoPor == null)
            return Result.fail(ErroIdentidade.CONVITE_INDISPONIVEL.mensagem());
        return Result.ok(new Convite(UUID.randomUUID(), agora, agora, null, empresaId, emailOpcional, hash,
                validade, limite, 0, null, criadoPor));
    }

    public static Convite reconstituir(UUID id, Instant createdAt, Instant updatedAt, OffsetDateTime deletedAt,
                                       UUID empresaId, EmailUsuario email, HashToken hashToken, Instant expiraEm,
                                       int maxUsos, int usos, Instant revogadoEm, UUID criadoPor) {
        return new Convite(id, createdAt, updatedAt, deletedAt, empresaId, email, hashToken, expiraEm, maxUsos,
                usos, revogadoEm, criadoPor);
    }

    /** Idempotente: revogar de novo não altera {@code revogadoEm}. */
    public void revogar(Instant agora) {
        if (revogadoEm == null)
            revogadoEm = agora;
    }

    public boolean estaUtilizavel(Instant agora) {
        return revogadoEm == null && agora.isBefore(expiraEm) && usos < maxUsos;
    }

    /** Convite com e-mail só aceita esse e-mail (RN-IA-06). */
    public boolean aceitaEmail(EmailUsuario informado) {
        return email == null || email.equals(informado);
    }

    public UUID empresaId() { return empresaId; }
    public EmailUsuario email() { return email; }
    public HashToken hashToken() { return hashToken; }
    public Instant expiraEm() { return expiraEm; }
    public int maxUsos() { return maxUsos; }
    public int usos() { return usos; }
    public Instant revogadoEm() { return revogadoEm; }
    public UUID criadoPor() { return criadoPor; }
}
