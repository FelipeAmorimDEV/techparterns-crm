package tech.techparterns.funil.domain.model;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

import tech.techparterns.shared.domain.CompanyId;
import tech.techparterns.shared.domain.Entity;
import tech.techparterns.shared.domain.Result;

public class MotivoPerda extends Entity {

    public static final String NOME_OBRIGATORIO = "Nome do motivo de perda é obrigatório";

    private final CompanyId companyId;
    private final String nome;
    private boolean ativo;

    private MotivoPerda(UUID id, CompanyId companyId, String nome, boolean ativo) {
        super(id);
        this.companyId = companyId;
        this.nome = nome;
        this.ativo = ativo;
    }

    private MotivoPerda(UUID id, Instant createdAt, Instant updatedAt, OffsetDateTime deletedAt,
                        CompanyId companyId, String nome, boolean ativo) {
        super(id, createdAt, updatedAt, deletedAt);
        this.companyId = companyId;
        this.nome = nome;
        this.ativo = ativo;
    }

    public static Result<MotivoPerda> tryCreate(String companyId, String nome, boolean ativo) {
        var companyIdResult = CompanyId.tryCreate(companyId);
        var nomeResult = nome(nome);
        var validacao = Result.combineAll(companyIdResult, nomeResult);
        if (validacao.isFailure())
            return Result.propagate(validacao);
        return Result.ok(new MotivoPerda(
                UUID.randomUUID(),
                companyIdResult.getValue(),
                nomeResult.getValue(),
                ativo));
    }

    public static MotivoPerda reconstituir(UUID id, Instant createdAt, Instant updatedAt, OffsetDateTime deletedAt,
                                           CompanyId companyId, String nome, boolean ativo) {
        return new MotivoPerda(id, createdAt, updatedAt, deletedAt, companyId, nome, ativo);
    }

    public void alterarAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    private static Result<String> nome(String nome) {
        if (nome == null || nome.isBlank())
            return Result.fail(NOME_OBRIGATORIO);
        return Result.ok(nome.trim());
    }

    public CompanyId companyId() {
        return companyId;
    }

    public String nome() {
        return nome;
    }

    public boolean ativo() {
        return ativo;
    }
}
