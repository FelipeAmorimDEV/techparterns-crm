package tech.techparterns.lead.domain.model;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

import tech.techparterns.shared.domain.CompanyId;
import tech.techparterns.shared.domain.Entity;
import tech.techparterns.shared.domain.Result;

public class Lead extends Entity {

    public static final String SEM_MEIO = "Nenhum meio de contato";

    private final CompanyId companyId;
    private final NomeContato nome;
    private final TelefoneBrasil telefone;
    private final TelefoneBrasil whatsapp;
    private final EmailContato email;
    private final OrigemLead origem;
    private final SituacaoLead situacao;
    private final String uf;
    private final String cidade;
    private final boolean quente;

    private Lead(UUID id, Instant createdAt, Instant updatedAt, OffsetDateTime deletedAt,
                 CompanyId companyId, NomeContato nome, TelefoneBrasil telefone,
                 TelefoneBrasil whatsapp, EmailContato email, OrigemLead origem,
                 SituacaoLead situacao, String uf, String cidade, boolean quente) {
        super(id, createdAt, updatedAt, deletedAt);
        this.companyId = companyId;
        this.nome = nome;
        this.telefone = telefone;
        this.whatsapp = whatsapp;
        this.email = email;
        this.origem = origem;
        this.situacao = situacao;
        this.uf = uf;
        this.cidade = cidade;
        this.quente = quente;
    }

    /** Telefone, WhatsApp e e-mail são opcionais (null = ausente), mas ao menos um é exigido. */
    public static Result<Lead> tryCreate(CompanyId companyId, NomeContato nome,
                                         TelefoneBrasil telefone, TelefoneBrasil whatsapp, EmailContato email) {
        if (companyId == null)
            return Result.fail(tech.techparterns.shared.domain.exception.ErroDominio.EMPRESA_OBRIGATORIA.mensagem());
        if (nome == null)
            return Result.fail(NomeContato.VAZIO);
        if (telefone == null && whatsapp == null && email == null)
            return Result.fail(SEM_MEIO);
        var agora = Instant.now();
        return Result.ok(new Lead(UUID.randomUUID(), agora, agora, null,
                companyId, nome, telefone, whatsapp, email, OrigemLead.IMPORTACAO,
                SituacaoLead.ABERTA, null, null, false));
    }

    public static Lead reconstituir(UUID id, Instant createdAt, Instant updatedAt, OffsetDateTime deletedAt,
                                    CompanyId companyId, NomeContato nome, TelefoneBrasil telefone,
                                    TelefoneBrasil whatsapp, EmailContato email, OrigemLead origem) {
        return reconstituir(id, createdAt, updatedAt, deletedAt, companyId, nome, telefone, whatsapp, email, origem,
                SituacaoLead.ABERTA, null, null, false);
    }

    public static Lead reconstituir(UUID id, Instant createdAt, Instant updatedAt, OffsetDateTime deletedAt,
                                    CompanyId companyId, NomeContato nome, TelefoneBrasil telefone,
                                    TelefoneBrasil whatsapp, EmailContato email, OrigemLead origem,
                                    SituacaoLead situacao, String uf, String cidade, boolean quente) {
        return new Lead(id, createdAt, updatedAt, deletedAt, companyId, nome, telefone, whatsapp, email, origem,
                situacao, uf, cidade, quente);
    }

    /**
     * Preenche somente os meios vazios. Sem mudança devolve esta mesma instância;
     * com mudança devolve outra instância com o mesmo id e createdAt e updatedAt = agora.
     */
    public Lead completarMeiosVazios(TelefoneBrasil novoTelefone, TelefoneBrasil novoWhatsapp,
                                     EmailContato novoEmail, Instant agora) {
        boolean preencheTelefone = telefone == null && novoTelefone != null;
        boolean preencheWhatsapp = whatsapp == null && novoWhatsapp != null;
        boolean preencheEmail = email == null && novoEmail != null;
        if (!preencheTelefone && !preencheWhatsapp && !preencheEmail)
            return this;
        return new Lead(getId(), getCreatedAt(), agora, getDeletedAt(), companyId, nome,
                preencheTelefone ? novoTelefone : telefone,
                preencheWhatsapp ? novoWhatsapp : whatsapp,
                preencheEmail ? novoEmail : email,
                origem, situacao, uf, cidade, quente);
    }

    /**
     * Situação final não muda. UF e cidade já vêm normalizadas pelo caso de uso.
     */
    public Result<Lead> comFatos(SituacaoLead novaSituacao, boolean novoQuente, String novaUf, String novaCidade,
                                 Instant agora) {
        if (situacao != SituacaoLead.ABERTA)
            return Result.fail("Situação final não muda");
        if (novaSituacao == null)
            return Result.fail("Situação inválida");
        return Result.ok(new Lead(getId(), getCreatedAt(), agora, getDeletedAt(), companyId, nome,
                telefone, whatsapp, email, origem, novaSituacao, novaUf, novaCidade, novoQuente));
    }

    /** Idempotente: lead já excluído permanece como está. */
    public void excluirLogicamente(OffsetDateTime quando) {
        if (isDeleted())
            return;
        setDeletedAt(quando);
    }

    public CompanyId companyId() { return companyId; }
    public NomeContato nome() { return nome; }
    public TelefoneBrasil telefone() { return telefone; }
    public TelefoneBrasil whatsapp() { return whatsapp; }
    public EmailContato email() { return email; }
    public OrigemLead origem() { return origem; }
    public SituacaoLead situacao() { return situacao; }
    public String uf() { return uf; }
    public String cidade() { return cidade; }
    public boolean quente() { return quente; }
}
