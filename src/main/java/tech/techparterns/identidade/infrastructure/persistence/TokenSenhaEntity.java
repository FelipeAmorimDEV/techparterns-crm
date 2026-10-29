package tech.techparterns.identidade.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "password_token")
public class TokenSenhaEntity extends BaseEntity {

    @Column(name = "user_id", nullable = false) public UUID usuarioId;
    @Column(name = "tipo", nullable = false) public String tipo;
    @Column(name = "token_hash", nullable = false) public String tokenHash;
    @Column(name = "expira_em", nullable = false) public Instant expiraEm;
    @Column(name = "usado_em") public Instant usadoEm;
    @Column(name = "invalidado_em") public Instant invalidadoEm;
}
