package tech.techparterns.shared.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import tech.techparterns.shared.domain.exception.ErroDominio;

class CompanyIdTest {

    @Test
    void tryCreateRejeitaVazio() {
        var result = CompanyId.tryCreate("  ");
        assertTrue(result.isFailure());
        assertEquals(ErroDominio.EMPRESA_OBRIGATORIA.mensagem(), result.getErrors().getFirst());
    }

    @Test
    void tryCreateNormaliza() {
        var result = CompanyId.tryCreate("  empresa-1  ");
        assertTrue(result.isOk());
        assertEquals("empresa-1", result.getValue().valor());
    }

    @Test
    void reconstituirNaoValida() {
        var id = CompanyId.reconstituir("  ");
        assertEquals("  ", id.valor());
    }
}
