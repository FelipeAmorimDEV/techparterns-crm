package tech.techparterns.identidade.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "company")
public class EmpresaEntity extends BaseEntity {

    @Column(name = "nome", nullable = false) public String nome;
    @Column(name = "status", nullable = false) public String status;
    @Column(name = "suspensa_em") public Instant suspensaEm;
    @Column(name = "motivo_suspensao") public String motivoSuspensao;
}
