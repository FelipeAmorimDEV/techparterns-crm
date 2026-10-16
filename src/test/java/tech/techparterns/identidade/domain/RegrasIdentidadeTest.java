package tech.techparterns.identidade.domain;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import tech.techparterns.identidade.domain.model.SenhaPlana;

class RegrasIdentidadeTest {

    @Test
    void senhaValida() {
        assertFalse(SenhaPlana.tryCreate("abc12345").isFailure());
    }

    @Test
    void senhaCurtaSemNumeroOuLetraFalha() {
        assertTrue(SenhaPlana.tryCreate("abc123").isFailure());
        assertTrue(SenhaPlana.tryCreate("abcdefgh").isFailure());
        assertTrue(SenhaPlana.tryCreate("12345678").isFailure());
        assertTrue(SenhaPlana.tryCreate(null).isFailure());
    }

    @Test
    void senhaAcimaDe72BytesFalha() {
        assertTrue(SenhaPlana.tryCreate("a1".repeat(37)).isFailure());
        assertFalse(SenhaPlana.tryCreate("a1".repeat(36)).isFailure());
    }

    @Test
    void toStringNaoVazaSenha() {
        assertFalse(SenhaPlana.tryCreate("abc12345").getValue().toString().contains("abc12345"));
    }
}
