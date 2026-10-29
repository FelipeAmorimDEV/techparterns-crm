package tech.techparterns.funil.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TipoEtapaTest {

    @Test
    void tresValoresExistem() {
        assertEquals(TipoEtapa.ABERTO, TipoEtapa.valueOf("ABERTO"));
        assertEquals(TipoEtapa.GANHO, TipoEtapa.valueOf("GANHO"));
        assertEquals(TipoEtapa.PERDIDO, TipoEtapa.valueOf("PERDIDO"));
        assertEquals(3, TipoEtapa.values().length);
    }
}
