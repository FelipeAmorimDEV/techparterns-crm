package tech.techparterns.distribuicao.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "lead_ownership")
public class LeadOwnershipEntity {

    @Id
    @Column(name = "lead_id")
    public UUID leadId;

    @Column(name = "company_id", nullable = false)
    public String companyId;

    @Column(name = "owner_user_id")
    public UUID ownerUserId;

    @Column(name = "atribuido_em")
    public Instant atribuidoEm;

    @Column(name = "parado", nullable = false)
    public boolean parado;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;
}
