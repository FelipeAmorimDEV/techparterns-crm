package tech.techparterns.funil.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class OrdemEtapaTest {

    @Test
    void nulaFalha() {
        assertEquals(java.util.List.of(OrdemEtapa.INVALIDA), OrdemEtapa.tryCreate(null).getErrors());
    }

    @Test
    void zeroFalha() {
        assertEquals(java.util.List.of(OrdemEtapa.INVALIDA), OrdemEtapa.tryCreate(0).getErrors());
    }

    @Test
    void negativaFalha() {
        assertEquals(java.util.List.of(OrdemEtapa.INVALIDA), OrdemEtapa.tryCreate(-1).getErrors());
    }

    @Test
    void umPassa() {
        var r = OrdemEtapa.tryCreate(1);
        assertTrue(r.isOk());
        assertEquals(1, r.getValue().valor());
    }
}
