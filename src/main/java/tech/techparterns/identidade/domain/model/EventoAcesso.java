package tech.techparterns.identidade.domain.model;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

import tech.techparterns.shared.domain.Entity;

/**
 * Evento de auditoria. Não possui campo para senha, token, link nem e-mail de terceiros:
 * {@code detalhe} recebe apenas texto curto escolhido pelo código.
 */
public class EventoAcesso extends Entity {

    private final String tipo;
    private final String resultado;
    private final TitularTipo titularTipo;
    private final UUID titularId;
    private final UUID empresaId;
    private final String ip;
    private final Instant ocorridoEm;
    private final String detalhe;

    private EventoAcesso(UUID id, Instant createdAt, Instant updatedAt, OffsetDateTime deletedAt, String tipo,
                         String resultado, TitularTipo titularTipo, UUID titularId, UUID empresaId, String ip,
                         Instant ocorridoEm, String detalhe) {
        super(id, createdAt, updatedAt, deletedAt);
        this.tipo = tipo;
        this.resultado = resultado;
        this.titularTipo = titularTipo;
        this.titularId = titularId;
        this.empresaId = empresaId;
        this.ip = ip;
        this.ocorridoEm = ocorridoEm;
        this.detalhe = detalhe;
    }

    public static EventoAcesso registrar(String tipo, String resultado, TitularTipo titularTipo, UUID titularId,
                                         UUID empresaId, String ip, Instant agora, String detalhe) {
        String d = detalhe != null && detalhe.length() > 255 ? detalhe.substring(0, 255) : detalhe;
        return new EventoAcesso(UUID.randomUUID(), agora, agora, null, tipo, resultado, titularTipo, titularId,
                empresaId, ip, agora, d);
    }

    public static EventoAcesso reconstituir(UUID id, Instant createdAt, Instant updatedAt, OffsetDateTime deletedAt,
                                            String tipo, String resultado, TitularTipo titularTipo, UUID titularId,
                                            UUID empresaId, String ip, Instant ocorridoEm, String detalhe) {
        return new EventoAcesso(id, createdAt, updatedAt, deletedAt, tipo, resultado, titularTipo, titularId,
                empresaId, ip, ocorridoEm, detalhe);
    }

    public String tipo() { return tipo; }
    public String resultado() { return resultado; }
    public TitularTipo titularTipo() { return titularTipo; }
    public UUID titularId() { return titularId; }
    public UUID empresaId() { return empresaId; }
    public String ip() { return ip; }
    public Instant ocorridoEm() { return ocorridoEm; }
    public String detalhe() { return detalhe; }
}
