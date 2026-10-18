package tech.techparterns.identidade.domain.model;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

import tech.techparterns.shared.domain.Entity;
import tech.techparterns.shared.domain.Result;

public class Empresa extends Entity {

    private NomeEmpresa nome;
    private StatusEmpresa status;
    private Instant suspensaEm;
    private MotivoSuspensao motivoSuspensao;

    private Empresa(UUID id, Instant createdAt, Instant updatedAt, OffsetDateTime deletedAt,
                    NomeEmpresa nome, StatusEmpresa status, Instant suspensaEm, MotivoSuspensao motivo) {
        super(id, createdAt, updatedAt, deletedAt);
        this.nome = nome;
        this.status = status;
        this.suspensaEm = suspensaEm;
        this.motivoSuspensao = motivo;
    }

    public static Result<Empresa> tryCreate(NomeEmpresa nome) {
        if (nome == null)
            return Result.fail(ErroIdentidade.EMPRESA_NOME_OBRIGATORIO.mensagem());
        var agora = Instant.now();
        return Result.ok(new Empresa(UUID.randomUUID(), agora, agora, null, nome, StatusEmpresa.ACTIVE, null, null));
    }

    public static Empresa reconstituir(UUID id, Instant createdAt, Instant updatedAt, OffsetDateTime deletedAt,
                                       NomeEmpresa nome, StatusEmpresa status, Instant suspensaEm,
                                       MotivoSuspensao motivo) {
        return new Empresa(id, createdAt, updatedAt, deletedAt, nome, status, suspensaEm, motivo);
    }

    public void renomear(NomeEmpresa novoNome) { this.nome = novoNome; }

    /** Não altera usuários (RN-IA-13). */
    public Result<Void> suspender(MotivoSuspensao motivo, Instant agora) {
        if (status == StatusEmpresa.SUSPENDED)
            return Result.fail(ErroIdentidade.ESTADO_EMPRESA_INVALIDO.mensagem());
        this.status = StatusEmpresa.SUSPENDED;
        this.suspensaEm = agora;
        this.motivoSuspensao = motivo;
        return Result.ok();
    }

    public Result<Void> reativar() {
        if (status == StatusEmpresa.ACTIVE)
            return Result.fail(ErroIdentidade.ESTADO_EMPRESA_INVALIDO.mensagem());
        this.status = StatusEmpresa.ACTIVE;
        this.suspensaEm = null;
        this.motivoSuspensao = null;
        return Result.ok();
    }

    public boolean estaAtiva() { return status == StatusEmpresa.ACTIVE; }

    public NomeEmpresa nome() { return nome; }
    public StatusEmpresa status() { return status; }
    public Instant suspensaEm() { return suspensaEm; }
    public MotivoSuspensao motivoSuspensao() { return motivoSuspensao; }
}
