package tech.techparterns.funil.domain.model;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

import tech.techparterns.shared.domain.Entity;
import tech.techparterns.shared.domain.Result;

public class HistoricoEtapa extends Entity {

    public static final String DESTINO_OBRIGATORIO = "Etapa de destino é obrigatória";
    public static final String QUANDO_OBRIGATORIO = "Instante da transição é obrigatório";

    private final UUID origemEtapaId;
    private final UUID destinoEtapaId;
    private final UUID userId;
    private final Instant quando;

    private HistoricoEtapa(UUID id, UUID origemEtapaId, UUID destinoEtapaId, UUID userId, Instant quando) {
        super(id);
        this.origemEtapaId = origemEtapaId;
        this.destinoEtapaId = destinoEtapaId;
        this.userId = userId;
        this.quando = quando;
    }

    private HistoricoEtapa(UUID id, Instant createdAt, Instant updatedAt, OffsetDateTime deletedAt,
                           UUID origemEtapaId, UUID destinoEtapaId, UUID userId, Instant quando) {
        super(id, createdAt, updatedAt, deletedAt);
        this.origemEtapaId = origemEtapaId;
        this.destinoEtapaId = destinoEtapaId;
        this.userId = userId;
        this.quando = quando;
    }

    public static Result<HistoricoEtapa> tryCreate(UUID origemEtapaId, UUID destinoEtapaId, UUID userId,
                                                   Instant quando) {
        var destinoResult = destinoEtapaId == null
                ? Result.<UUID>fail(DESTINO_OBRIGATORIO)
                : Result.ok(destinoEtapaId);
        var quandoResult = quando == null ? Result.<Instant>fail(QUANDO_OBRIGATORIO) : Result.ok(quando);
        var validacao = Result.combineAll(destinoResult, quandoResult);
        if (validacao.isFailure())
            return Result.propagate(validacao);
        return Result.ok(new HistoricoEtapa(
                UUID.randomUUID(),
                origemEtapaId,
                destinoResult.getValue(),
                userId,
                quandoResult.getValue()));
    }

    public static HistoricoEtapa reconstituir(UUID id, Instant createdAt, Instant updatedAt, OffsetDateTime deletedAt,
                                              UUID origemEtapaId, UUID destinoEtapaId, UUID userId, Instant quando) {
        return new HistoricoEtapa(id, createdAt, updatedAt, deletedAt, origemEtapaId, destinoEtapaId, userId, quando);
    }

    public UUID origemEtapaId() {
        return origemEtapaId;
    }

    public UUID destinoEtapaId() {
        return destinoEtapaId;
    }

    public UUID userId() {
        return userId;
    }

    public Instant quando() {
        return quando;
    }
}
