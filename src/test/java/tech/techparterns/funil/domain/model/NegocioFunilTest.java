package tech.techparterns.funil.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class NegocioFunilTest {

    @Test
    void tryCreateDeixaRoteiroDataEValoresNulos() {
        var leadId = UUID.randomUUID();
        var etapaId = UUID.randomUUID();
        var entrada = Instant.parse("2026-05-01T12:00:00Z");
        var r = NegocioFunil.tryCreate(leadId, etapaId, entrada);
        assertTrue(r.isOk());
        var negocio = r.getValue();
        assertEquals(leadId, negocio.leadId());
        assertEquals(etapaId, negocio.etapaId());
        assertEquals(entrada, negocio.entrouNaEtapa());
        assertNull(negocio.decisor());
        assertNull(negocio.dorPrincipal());
        assertNull(negocio.usuariosPrevistos());
        assertNull(negocio.prazoDecisao());
        assertNull(negocio.orcamento());
        assertNull(negocio.dataDemo());
        assertNull(negocio.valorEstimado());
        assertNull(negocio.valorFechado());
    }

    @Test
    void leadNuloFalha() {
        var r = NegocioFunil.tryCreate(null, UUID.randomUUID(), Instant.parse("2026-05-01T12:00:00Z"));
        assertEquals(java.util.List.of(NegocioFunil.LEAD_OBRIGATORIO), r.getErrors());
    }

    @Test
    void reconstituirPreservaObservacaoLonga() {
        var observacao = "o".repeat(301);
        var id = UUID.randomUUID();
        var criado = Instant.parse("2026-05-01T10:00:00Z");
        var atualizado = Instant.parse("2026-05-02T10:00:00Z");
        var excluido = OffsetDateTime.parse("2026-05-03T10:00:00Z");
        var leadId = UUID.randomUUID();
        var etapaId = UUID.randomUUID();
        var entrada = Instant.parse("2026-05-01T12:00:00Z");
        var negocio = NegocioFunil.reconstituir(
                id, criado, atualizado, excluido,
                leadId, etapaId, entrada,
                null, null, null, null, null, null,
                null, null, null, null, null, null,
                observacao);
        assertEquals(criado, negocio.getCreatedAt());
        assertEquals(excluido, negocio.getDeletedAt());
        assertEquals(observacao, negocio.observacao());
        assertEquals(301, negocio.observacao().length());
    }
}
