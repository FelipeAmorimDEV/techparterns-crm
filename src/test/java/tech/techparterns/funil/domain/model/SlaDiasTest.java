package tech.techparterns.funil.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SlaDiasTest {

    @Test
    void nuloPassaEFicaVazio() {
        var r = SlaDias.tryCreate(null);
        assertTrue(r.isOk());
        assertTrue(r.getValue().vazio());
    }

    @Test
    void zeroFalha() {
        assertEquals(java.util.List.of(SlaDias.INVALIDO), SlaDias.tryCreate(0).getErrors());
    }

    @Test
    void negativoFalha() {
        assertEquals(java.util.List.of(SlaDias.INVALIDO), SlaDias.tryCreate(-1).getErrors());
    }

    @Test
    void positivoPassa() {
        var r = SlaDias.tryCreate(2);
        assertTrue(r.isOk());
        assertFalse(r.getValue().vazio());
        assertEquals(2, r.getValue().valor());
    }
}
