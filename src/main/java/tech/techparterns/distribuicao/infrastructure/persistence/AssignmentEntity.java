package tech.techparterns.distribuicao.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "assignment")
public class AssignmentEntity {

    @Id
    @Column(name = "id")
    public UUID id;

    @Column(name = "company_id", nullable = false)
    public String companyId;

    @Column(name = "lead_id", nullable = false)
    public UUID leadId;

    @Column(name = "previous_owner_id")
    public UUID previousOwnerId;

    @Column(name = "new_owner_id")
    public UUID newOwnerId;

    @Column(name = "regra", nullable = false)
    public String regra;

    @Column(name = "motivo")
    public String motivo;

    @Column(name = "por_user_id")
    public UUID porUserId;

    @Column(name = "ocorrido_em", nullable = false)
    public Instant ocorridoEm;
}
