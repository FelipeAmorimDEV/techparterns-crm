package tech.techparterns.funil.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TextoPlanoTest {

    @Test
    void vazioDepoisDoTrimFalha() {
        assertEquals(java.util.List.of(TextoPlano.VAZIO), TextoPlano.tryCreate("").getErrors());
        assertEquals(java.util.List.of(TextoPlano.VAZIO), TextoPlano.tryCreate("   ").getErrors());
    }

    @Test
    void acimaDe40Falha() {
        assertEquals(java.util.List.of(TextoPlano.LONGO), TextoPlano.tryCreate("a".repeat(41)).getErrors());
    }

    @Test
    void cortaPontasEFicaComCincoCaracteres() {
        var r = TextoPlano.tryCreate("  plano  ");
        assertTrue(r.isOk());
        assertEquals("plano", r.getValue().valor());
        assertEquals(5, r.getValue().valor().length());
    }

    @Test
    void mesmoTextoPodeSerCriadoDuasVezes() {
        assertTrue(TextoPlano.tryCreate("plano").isOk());
        assertTrue(TextoPlano.tryCreate("plano").isOk());
    }
}
