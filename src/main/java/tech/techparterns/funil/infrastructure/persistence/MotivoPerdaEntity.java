package tech.techparterns.funil.infrastructure.persistence;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "funil_motivo_perda")
public class MotivoPerdaEntity {

    @Id
    @Column(name = "id")
    public UUID id;

    @Column(name = "company_id", nullable = false)
    public String companyId;

    @Column(name = "nome", nullable = false)
    public String nome;

    @Column(name = "ativo", nullable = false)
    public boolean ativo;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;

    @Column(name = "deleted_at")
    public OffsetDateTime deletedAt;
}
