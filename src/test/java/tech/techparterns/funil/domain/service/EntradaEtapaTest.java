package tech.techparterns.funil.domain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

class EntradaEtapaTest {

    @Test
    void qualificacaoComQualquerItemNuloFalha() {
        Boolean[] preenchidos = {true, false, true, false, true};
        for (int i = 0; i < 5; i++) {
            Boolean[] itens = preenchidos.clone();
            itens[i] = null;
            var r = EntradaEtapa.avaliar(true, false, itens[0], itens[1], itens[2], itens[3], itens[4], null);
            assertEquals(List.of(EntradaEtapa.QUALIFICACAO_INCOMPLETA), r.getErrors(), "item " + i);
        }
    }

    @Test
    void qualquerCombinacaoDeSimENaoPassa() {
        for (int mascara = 0; mascara < 32; mascara++) {
            var r = EntradaEtapa.avaliar(
                    true,
                    false,
                    bit(mascara, 0),
                    bit(mascara, 1),
                    bit(mascara, 2),
                    bit(mascara, 3),
                    bit(mascara, 4),
                    null);
            assertTrue(r.isOk(), "mascara " + mascara);
        }
    }

    @Test
    void demoSemDataFalhaEComDataPassa() {
        var semData = EntradaEtapa.avaliar(false, true, null, null, null, null, null, null);
        assertEquals(List.of(EntradaEtapa.DEMO_OBRIGATORIA), semData.getErrors());

        var comData = EntradaEtapa.avaliar(false, true, null, null, null, null, null, LocalDate.of(2026, 10, 28));
        assertTrue(comData.isOk());
    }

    @Test
    void flagsDesligadasAceitamDadosAusentes() {
        var r = EntradaEtapa.avaliar(false, false, null, null, null, null, null, null);
        assertTrue(r.isOk());
    }

    @Test
    void asDuasFalhasNoMesmoResult() {
        var r = EntradaEtapa.avaliar(true, true, null, null, null, null, null, null);
        assertEquals(2, r.getErrors().size());
        assertTrue(r.getErrors().contains(EntradaEtapa.QUALIFICACAO_INCOMPLETA));
        assertTrue(r.getErrors().contains(EntradaEtapa.DEMO_OBRIGATORIA));
    }

    private static boolean bit(int mascara, int indice) {
        return ((mascara >> indice) & 1) == 1;
    }
}
