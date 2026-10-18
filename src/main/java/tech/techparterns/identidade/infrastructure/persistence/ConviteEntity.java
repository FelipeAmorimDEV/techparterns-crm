package tech.techparterns.identidade.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "invite")
public class ConviteEntity extends BaseEntity {

    @Column(name = "company_id", nullable = false) public UUID empresaId;
    @Column(name = "email") public String email;
    @Column(name = "token_hash", nullable = false) public String tokenHash;
    @Column(name = "expira_em", nullable = false) public Instant expiraEm;
    @Column(name = "max_usos", nullable = false) public int maxUsos;
    @Column(name = "usos", nullable = false) public int usos;
    @Column(name = "revogado_em") public Instant revogadoEm;
    @Column(name = "criado_por", nullable = false) public UUID criadoPor;
}
