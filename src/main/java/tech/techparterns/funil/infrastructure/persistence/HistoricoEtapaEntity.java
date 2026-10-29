package tech.techparterns.funil.infrastructure.persistence;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "funil_historico_etapa")
public class HistoricoEtapaEntity {

    @Id
    @Column(name = "id")
    public UUID id;

    @Column(name = "origem_etapa_id")
    public UUID origemEtapaId;

    @Column(name = "destino_etapa_id", nullable = false)
    public UUID destinoEtapaId;

    @Column(name = "user_id")
    public UUID userId;

    @Column(name = "instante", nullable = false)
    public Instant instante;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;

    @Column(name = "deleted_at")
    public OffsetDateTime deletedAt;
}
