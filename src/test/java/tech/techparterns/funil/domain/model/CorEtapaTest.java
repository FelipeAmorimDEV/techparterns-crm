package tech.techparterns.funil.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CorEtapaTest {

    @Test
    void maiusculaEMinusculaPassamESaoIguais() {
        var maiuscula = CorEtapa.tryCreate("#1A2B3C");
        var minuscula = CorEtapa.tryCreate("#1a2b3c");
        assertTrue(maiuscula.isOk());
        assertTrue(minuscula.isOk());
        assertEquals(maiuscula.getValue(), minuscula.getValue());
        assertEquals("#1a2b3c", minuscula.getValue().valor());
    }

    @Test
    void curtaFalha() {
        assertEquals(java.util.List.of(CorEtapa.INVALIDA), CorEtapa.tryCreate("#FFF").getErrors());
    }

    @Test
    void oitoDigitosFalha() {
        assertEquals(java.util.List.of(CorEtapa.INVALIDA), CorEtapa.tryCreate("#1A2B3CFF").getErrors());
    }

    @Test
    void textoSemFormatoFalha() {
        assertEquals(java.util.List.of(CorEtapa.INVALIDA), CorEtapa.tryCreate("vermelho").getErrors());
    }
}
