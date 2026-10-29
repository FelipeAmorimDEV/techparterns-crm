package tech.techparterns.identidade.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "refresh_token")
public class RefreshTokenEntity extends BaseEntity {

    @Column(name = "titular_tipo", nullable = false) public String titularTipo;
    @Column(name = "titular_id", nullable = false) public UUID titularId;
    @Column(name = "familia_id", nullable = false) public UUID familiaId;
    @Column(name = "token_hash", nullable = false) public String tokenHash;
    @Column(name = "expira_em", nullable = false) public Instant expiraEm;
    @Column(name = "rotacionado_em") public Instant rotacionadoEm;
    @Column(name = "revogado_em") public Instant revogadoEm;
}
