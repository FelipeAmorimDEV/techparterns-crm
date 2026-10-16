package tech.techparterns.identidade.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "envio_email")
public class EnvioEmailEntity extends BaseEntity {

    @Column(name = "user_id", nullable = false) public UUID usuarioId;
    @Column(name = "tipo", nullable = false) public String tipo;
    @Column(name = "status", nullable = false) public String status;
    @Column(name = "tentativas", nullable = false) public int tentativas;
}
