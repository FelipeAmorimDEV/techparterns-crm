package tech.techparterns.lead.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class NomeContatoTest {

    @Test
    void cortaPontas() {
        assertEquals("Ana", NomeContato.tryCreate("  Ana  ").getValue().valor());
    }

    @Test
    void vazioFalha() {
        var r = NomeContato.tryCreate("   ");
        assertEquals(java.util.List.of("Nome do contato vazio"), r.getErrors());
    }

    @Test
    void nuloFalha() {
        assertTrue(NomeContato.tryCreate(null).isFailure());
    }

    @Test
    void limite120Aceito() {
        assertTrue(NomeContato.tryCreate(" " + "a".repeat(120) + " ").isOk());
    }

    @Test
    void acima120Falha() {
        var r = NomeContato.tryCreate("a".repeat(121));
        assertEquals(java.util.List.of("Nome do contato acima de 120 caracteres"), r.getErrors());
    }

    @Test
    void reconstituirNaoValida() {
        assertEquals("", NomeContato.reconstituir("").valor());
    }
}
