package tech.techparterns.lead.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import tech.techparterns.shared.domain.CompanyId;

class LeadTest {

    private static final CompanyId EMPRESA = CompanyId.reconstituir("emp-1");

    private static NomeContato nome() { return NomeContato.reconstituir("Ana"); }
    private static TelefoneBrasil tel() { return TelefoneBrasil.reconstituir("+5511988887777"); }
    private static TelefoneBrasil wpp() { return TelefoneBrasil.reconstituir("+552132165498"); }
    private static EmailContato email() { return EmailContato.reconstituir("ana@x.com"); }

    @Test
    void criaComOrigemImportacao() {
        var r = Lead.tryCreate(EMPRESA, nome(), tel(), null, null);
        assertTrue(r.isOk());
        var lead = r.getValue();
        assertEquals(OrigemLead.IMPORTACAO, lead.origem());
        assertEquals(EMPRESA, lead.companyId());
        assertNull(lead.whatsapp());
        assertFalse(lead.isDeleted());
    }

    @Test
    void semMeioFalha() {
        assertTrue(Lead.tryCreate(EMPRESA, nome(), null, null, null).isFailure());
    }

    @Test
    void reconstituirPreservaAuditoria() {
        var id = UUID.randomUUID();
        var c = Instant.parse("2026-01-01T00:00:00Z");
        var u = Instant.parse("2026-02-01T00:00:00Z");
        var lead = Lead.reconstituir(id, c, u, null, EMPRESA, nome(), tel(), null, null, OrigemLead.IMPORTACAO);
        assertEquals(id, lead.getId());
        assertEquals(c, lead.getCreatedAt());
        assertEquals(u, lead.getUpdatedAt());
    }

    @Test
    void completarPreencheSoVazios() {
        var original = Lead.tryCreate(EMPRESA, nome(), tel(), null, null).getValue();
        var agora = Instant.now().plusSeconds(60);
        var outroTel = TelefoneBrasil.reconstituir("+5521999999999");
        var novo = original.completarMeiosVazios(outroTel, wpp(), email(), agora);
        assertNotSame(original, novo);
        assertEquals(original.getId(), novo.getId());
        assertEquals(original.getCreatedAt(), novo.getCreatedAt());
        assertEquals(agora, novo.getUpdatedAt());
        assertEquals(tel(), novo.telefone());
        assertEquals(wpp(), novo.whatsapp());
        assertEquals(email(), novo.email());
        assertEquals(original.nome(), novo.nome());
        assertEquals(OrigemLead.IMPORTACAO, novo.origem());
    }

    @Test
    void completarSemMudancaDevolveMesmaInstancia() {
        var original = Lead.tryCreate(EMPRESA, nome(), tel(), wpp(), email()).getValue();
        var mesmo = original.completarMeiosVazios(tel(), wpp(), email(), Instant.now().plusSeconds(60));
        assertSame(original, mesmo);
        var soTel = Lead.tryCreate(EMPRESA, nome(), tel(), null, null).getValue();
        assertSame(soTel, soTel.completarMeiosVazios(null, null, null, Instant.now()));
    }

    @Test
    void segundaExclusaoNaoFalha() {
        var lead = Lead.tryCreate(EMPRESA, nome(), tel(), null, null).getValue();
        var primeira = OffsetDateTime.now();
        lead.excluirLogicamente(primeira);
        assertTrue(lead.isDeleted());
        assertDoesNotThrow(() -> lead.excluirLogicamente(primeira.plusDays(1)));
        assertEquals(primeira, lead.getDeletedAt());
    }
}
