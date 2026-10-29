package tech.techparterns.funil.domain.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import tech.techparterns.funil.domain.model.SlaDias;

class AtrasoEtapaTest {

    private static final Instant ENTRADA = Instant.parse("2026-07-01T00:00:00Z");

    @Test
    void slaVazioNuncaAtrasa() {
        var sla = SlaDias.tryCreate(null).getValue();
        assertFalse(AtrasoEtapa.atrasado(ENTRADA, ENTRADA, sla));
        assertFalse(AtrasoEtapa.atrasado(ENTRADA, ENTRADA.plus(Duration.ofDays(400)), sla));
        assertFalse(AtrasoEtapa.atrasado(ENTRADA, ENTRADA.minus(Duration.ofHours(1)), sla));
    }

    @Test
    void slaDeDoisDiasSoDepoisDeQuarentaEOitoHoras() {
        var sla = SlaDias.tryCreate(2).getValue();
        var limite = ENTRADA.plus(Duration.ofHours(48));
        assertFalse(AtrasoEtapa.atrasado(ENTRADA, limite, sla));
        assertTrue(AtrasoEtapa.atrasado(ENTRADA, limite.plusNanos(1), sla));
        assertFalse(AtrasoEtapa.atrasado(ENTRADA, limite.minusNanos(1), sla));
    }
}
