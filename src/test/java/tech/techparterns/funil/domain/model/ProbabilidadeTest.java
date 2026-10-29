package tech.techparterns.funil.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ProbabilidadeTest {

    @Test
    void zeroPassa() {
        var r = Probabilidade.tryCreate(0);
        assertTrue(r.isOk());
        assertEquals(0, r.getValue().valor());
    }

    @Test
    void nulaFalha() {
        var r = Probabilidade.tryCreate(null);
        assertEquals(java.util.List.of(Probabilidade.OBRIGATORIA), r.getErrors());
    }

    @Test
    void menosUmFalha() {
        var r = Probabilidade.tryCreate(-1);
        assertEquals(java.util.List.of(Probabilidade.FORA_DA_FAIXA), r.getErrors());
    }

    @Test
    void centoEUmFalha() {
        var r = Probabilidade.tryCreate(101);
        assertEquals(java.util.List.of(Probabilidade.FORA_DA_FAIXA), r.getErrors());
    }
}
