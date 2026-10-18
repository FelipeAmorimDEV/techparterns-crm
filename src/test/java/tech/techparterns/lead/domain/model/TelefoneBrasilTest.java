package tech.techparterns.lead.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

class TelefoneBrasilTest {

    @Test
    void celularNormaliza() {
        assertEquals("+5511988887777", TelefoneBrasil.tryCreate("+55 (11) 98888-7777").getValue().valor());
    }

    @Test
    void fixoNormaliza() {
        assertEquals("+551132165498", TelefoneBrasil.tryCreate("+55 (11) 3216-5498").getValue().valor());
    }

    @Test
    void semMaisPrefixa() {
        assertEquals("+5511988887777", TelefoneBrasil.tryCreate("55 11 98888.7777").getValue().valor());
    }

    @Test
    void dddInvalidoFalha() {
        assertEquals(List.of("Telefone inválido"), TelefoneBrasil.tryCreate("+55 (20) 98888-7777").getErrors());
    }

    @Test
    void nonoDigitoSemNoveFalha() {
        assertTrue(TelefoneBrasil.tryCreate("+55 (11) 18888-7777").isFailure());
    }

    @Test
    void semCodigoPaisFalha() {
        assertTrue(TelefoneBrasil.tryCreate("(11) 98888-7777").isFailure());
    }

    @Test
    void whatsappUsaMensagemPropria() {
        assertEquals(List.of("WhatsApp inválido"), TelefoneBrasil.tryCreateWhatsapp("(11) 98888-7777").getErrors());
        assertEquals("+552132165498", TelefoneBrasil.tryCreateWhatsapp("+55 (21) 3216-5498").getValue().valor());
    }

    @Test
    void reconstituirNaoValida() {
        assertEquals("abc", TelefoneBrasil.reconstituir("abc").valor());
    }
}
