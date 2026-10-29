package tech.techparterns.funil.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EventosFunilTest {

    @Test
    void recordsSemComponentes() {
        assertTrue(LeadMudouEtapa.class.isRecord());
        assertTrue(LeadGanho.class.isRecord());
        assertTrue(LeadPerdido.class.isRecord());
        assertEquals(0, LeadMudouEtapa.class.getRecordComponents().length);
        assertEquals(0, LeadGanho.class.getRecordComponents().length);
        assertEquals(0, LeadPerdido.class.getRecordComponents().length);
        new LeadMudouEtapa();
        new LeadGanho();
        new LeadPerdido();
    }
}
