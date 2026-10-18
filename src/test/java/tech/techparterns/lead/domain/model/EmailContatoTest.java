package tech.techparterns.lead.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class EmailContatoTest {

    @Test
    void normalizaMinusculasECorta() {
        assertEquals("ana@empresa.com", EmailContato.tryCreate(" Ana@Empresa.COM ").getValue().valor());
    }

    @Test
    void naoValidaFormato() {
        assertTrue(EmailContato.tryCreate("semarroba").isOk());
    }

    @Test
    void vazioFalha() {
        assertTrue(EmailContato.tryCreate("  ").isFailure());
    }

    @Test
    void reconstituirNaoValida() {
        assertEquals("X", EmailContato.reconstituir("X").valor());
    }
}
