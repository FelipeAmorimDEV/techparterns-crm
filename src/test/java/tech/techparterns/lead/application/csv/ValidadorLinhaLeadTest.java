package tech.techparterns.lead.application.csv;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

class ValidadorLinhaLeadTest {

    private static LinhaCsv l(String n, String t, String w, String e) { return new LinhaCsv(2, n, t, w, e); }

    @Test
    void linhaValidaNormaliza() {
        var r = ValidadorLinhaLead.validar(l(" Ana ", "+55 (11) 98888-7777", "", " Ana@Empresa.COM "));
        assertTrue(r.isOk());
        var v = r.getValue();
        assertEquals("Ana", v.nome().valor());
        assertEquals("+5511988887777", v.telefone().valor());
        assertNull(v.whatsapp());
        assertEquals("ana@empresa.com", v.email().valor());
    }

    @Test
    void nomeVazioENomeLongo() {
        assertEquals(List.of("Nome do contato vazio"), ValidadorLinhaLead.validar(l(" ", "", "", "a@x.com")).getErrors());
        assertEquals(List.of("Nome do contato acima de 120 caracteres"),
                ValidadorLinhaLead.validar(l("a".repeat(121), "", "", "a@x.com")).getErrors());
        assertTrue(ValidadorLinhaLead.validar(l("a".repeat(120), "", "", "a@x.com")).isOk());
    }

    @Test
    void telefoneInvalidoRejeitaMesmoComEmailValido() {
        var r = ValidadorLinhaLead.validar(l("Ana", "(11) 98888-7777", "", "a@x.com"));
        assertEquals(List.of("Telefone inválido"), r.getErrors());
    }

    @Test
    void whatsappInvalido() {
        var r = ValidadorLinhaLead.validar(l("Ana", "", "123", "a@x.com"));
        assertEquals(List.of("WhatsApp inválido"), r.getErrors());
    }

    @Test
    void semMeioEMotivosJuntosNaOrdem() {
        assertEquals(List.of("Nenhum meio de contato"), ValidadorLinhaLead.validar(l("Ana", "", " ", "")).getErrors());
        var r = ValidadorLinhaLead.validar(l("", "x", "y", ""));
        assertEquals(List.of("Nome do contato vazio", "Telefone inválido", "WhatsApp inválido", "Nenhum meio de contato"),
                r.getErrors());
        assertEquals("Nome do contato vazio; Telefone inválido; WhatsApp inválido; Nenhum meio de contato",
                String.join("; ", r.getErrors()));
    }
}
