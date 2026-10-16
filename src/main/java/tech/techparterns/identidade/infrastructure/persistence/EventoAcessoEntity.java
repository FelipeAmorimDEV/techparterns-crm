package tech.techparterns.identidade.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "evento_acesso")
public class EventoAcessoEntity extends BaseEntity {

    @Column(name = "tipo", nullable = false) public String tipo;
    @Column(name = "resultado", nullable = false) public String resultado;
    @Column(name = "titular_tipo") public String titularTipo;
    @Column(name = "titular_id") public UUID titularId;
    @Column(name = "company_id") public UUID empresaId;
    @Column(name = "ip") public String ip;
    @Column(name = "ocorrido_em", nullable = false) public Instant ocorridoEm;
    @Column(name = "detalhe") public String detalhe;
}
