package tech.techparterns.lead.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import tech.techparterns.shared.domain.CompanyId;

class LeadDistribuicaoTest {

    @Test
    void leadNovoNasceAbertoSemTerritorioESemQuente() {
        var lead = Lead.tryCreate(CompanyId.reconstituir("e"), NomeContato.reconstituir("Ana"),
                TelefoneBrasil.reconstituir("+5511988887777"), null, null).getValue();
        assertEquals(SituacaoLead.ABERTA, lead.situacao());
        assertFalse(lead.quente());
        assertNull(lead.uf());
        assertNull(lead.cidade());
    }

    @Test
    void reconstituirDevolveOsQuatroFatos() {
        var agora = Instant.parse("2026-03-01T00:00:00Z");
        var lead = Lead.reconstituir(java.util.UUID.randomUUID(), agora, agora, null, CompanyId.reconstituir("e"),
                NomeContato.reconstituir("Ana"), null, null, EmailContato.reconstituir("a@x.com"), OrigemLead.IMPORTACAO,
                SituacaoLead.GANHO, "SP", "São Paulo", true);
        assertEquals(SituacaoLead.GANHO, lead.situacao());
        assertEquals("SP", lead.uf());
        assertEquals("São Paulo", lead.cidade());
        assertTrue(lead.quente());
    }

    @Test
    void situacaoFinalNaoMuda() {
        var original = Lead.tryCreate(CompanyId.reconstituir("e"), NomeContato.reconstituir("Ana"),
                TelefoneBrasil.reconstituir("+5511988887777"), null, null).getValue();
        var ganho = original.comFatos(SituacaoLead.GANHO, false, null, null, Instant.now()).getValue();
        assertTrue(ganho.comFatos(SituacaoLead.ABERTA, true, "RJ", "Niterói", Instant.now()).isFailure());
    }
}
