package tech.techparterns.funil.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class HistoricoEtapaTest {

    @Test
    void origemEUsuarioNulosPassam() {
        var destino = UUID.randomUUID();
        var quando = Instant.parse("2026-06-01T08:00:00Z");
        var r = HistoricoEtapa.tryCreate(null, destino, null, quando);
        assertTrue(r.isOk());
        assertNull(r.getValue().origemEtapaId());
        assertNull(r.getValue().userId());
        assertEquals(destino, r.getValue().destinoEtapaId());
        assertEquals(quando, r.getValue().quando());
    }

    @Test
    void destinoNuloFalha() {
        var r = HistoricoEtapa.tryCreate(UUID.randomUUID(), null, UUID.randomUUID(), Instant.parse("2026-06-01T08:00:00Z"));
        assertEquals(java.util.List.of(HistoricoEtapa.DESTINO_OBRIGATORIO), r.getErrors());
    }
}
