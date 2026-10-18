package tech.techparterns.lead.infrastructure.persistence;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "lead")
public class LeadEntity {

    @Id
    @Column(name = "id")
    public UUID id;

    @Column(name = "company_id", nullable = false)
    public String companyId;

    @Column(name = "nome_contato", nullable = false, length = 120)
    public String nomeContato;

    @Column(name = "telefone")
    public String telefone;

    @Column(name = "whatsapp")
    public String whatsapp;

    @Column(name = "email")
    public String email;

    @Column(name = "origem", nullable = false)
    public String origem;

    @Column(name = "situacao", nullable = false)
    public String situacao;

    @Column(name = "uf")
    public String uf;

    @Column(name = "cidade")
    public String cidade;

    @Column(name = "quente", nullable = false)
    public boolean quente;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;

    @Column(name = "deleted_at")
    public OffsetDateTime deletedAt;
}
