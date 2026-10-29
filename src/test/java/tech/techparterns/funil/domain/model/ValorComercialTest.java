package tech.techparterns.funil.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class ValorComercialTest {

    @Test
    void negativoFalha() {
        assertEquals(java.util.List.of(ValorComercial.INVALIDO),
                ValorComercial.tryCreate(new BigDecimal("-1")).getErrors());
    }

    @Test
    void nuloFalha() {
        assertEquals(java.util.List.of(ValorComercial.INVALIDO), ValorComercial.tryCreate(null).getErrors());
    }

    @Test
    void zeroPassa() {
        var r = ValorComercial.tryCreate(BigDecimal.ZERO);
        assertTrue(r.isOk());
        assertEquals(0, r.getValue().valor().compareTo(new BigDecimal("0.00")));
        assertEquals(2, r.getValue().valor().scale());
    }

    @Test
    void escalaZeroUmEDoisGuardamEscalaDoisSemAlterarDigito() {
        var escalaZero = ValorComercial.tryCreate(new BigDecimal("100"));
        var escalaUm = ValorComercial.tryCreate(new BigDecimal("100.5"));
        var escalaDois = ValorComercial.tryCreate(new BigDecimal("100.00"));

        assertTrue(escalaZero.isOk());
        assertTrue(escalaUm.isOk());
        assertTrue(escalaDois.isOk());

        assertEquals(2, escalaZero.getValue().valor().scale());
        assertEquals(2, escalaUm.getValue().valor().scale());
        assertEquals(2, escalaDois.getValue().valor().scale());
        assertEquals("100.00", escalaZero.getValue().valor().toPlainString());
        assertEquals("100.50", escalaUm.getValue().valor().toPlainString());
        assertEquals("100.00", escalaDois.getValue().valor().toPlainString());
    }
}
