package tech.techparterns.identidade.domain.model;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

import tech.techparterns.shared.domain.Entity;

/** Registro do envio de e-mail. Não guarda corpo nem link (RN-IA-20). */
public class EnvioEmail extends Entity {

    private final UUID usuarioId;
    private final TipoTokenSenha tipo;
    private StatusEnvioEmail status;
    private int tentativas;

    private EnvioEmail(UUID id, Instant createdAt, Instant updatedAt, OffsetDateTime deletedAt, UUID usuarioId,
                       TipoTokenSenha tipo, StatusEnvioEmail status, int tentativas) {
        super(id, createdAt, updatedAt, deletedAt);
        this.usuarioId = usuarioId;
        this.tipo = tipo;
        this.status = status;
        this.tentativas = tentativas;
    }

    public static EnvioEmail pendente(UUID usuarioId, TipoTokenSenha tipo, Instant agora) {
        return new EnvioEmail(UUID.randomUUID(), agora, agora, null, usuarioId, tipo, StatusEnvioEmail.PENDENTE, 0);
    }

    public static EnvioEmail reconstituir(UUID id, Instant createdAt, Instant updatedAt, OffsetDateTime deletedAt,
                                          UUID usuarioId, TipoTokenSenha tipo, StatusEnvioEmail status,
                                          int tentativas) {
        return new EnvioEmail(id, createdAt, updatedAt, deletedAt, usuarioId, tipo, status, tentativas);
    }

    public void registrarSucesso() {
        this.tentativas++;
        this.status = StatusEnvioEmail.ENVIADO;
    }

    public void registrarFalha(boolean ultimaTentativa) {
        this.tentativas++;
        if (ultimaTentativa)
            this.status = StatusEnvioEmail.FALHA;
    }

    public UUID usuarioId() { return usuarioId; }
    public TipoTokenSenha tipo() { return tipo; }
    public StatusEnvioEmail status() { return status; }
    public int tentativas() { return tentativas; }
}
