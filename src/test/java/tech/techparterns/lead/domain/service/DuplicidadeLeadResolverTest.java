package tech.techparterns.lead.domain.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import tech.techparterns.lead.domain.model.*;
import tech.techparterns.shared.domain.CompanyId;

class DuplicidadeLeadResolverTest {

    private static final CompanyId EMPRESA = CompanyId.reconstituir("e");
    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");

    private static TelefoneBrasil tel(String v) { return v == null ? null : TelefoneBrasil.reconstituir(v); }
    private static EmailContato mail(String v) { return v == null ? null : EmailContato.reconstituir(v); }

    private static Lead lead(String t, String w, String e, Instant criado, Instant atualizado, boolean excluido) {
        return Lead.reconstituir(UUID.randomUUID(), criado, atualizado,
                excluido ? OffsetDateTime.now() : null, EMPRESA, NomeContato.reconstituir("X"),
                tel(t), tel(w), mail(e), OrigemLead.IMPORTACAO);
    }

    @Test
    void telefonePrevaleceSobreEmail() {
        var porTel = lead("+5511988887777", null, "a@x.com", T0, T0, false);
        var porMail = lead("+5521999999999", null, "b@x.com", T0, T0.plusSeconds(100), false);
        var r = DuplicidadeLeadResolver.resolver(tel("+5511988887777"), mail("b@x.com"), List.of(porTel, porMail));
        assertSame(porTel, r.orElseThrow());
    }

    @Test
    void caiParaEmailQuandoTelefoneNaoIdentifica() {
        var porMail = lead(null, null, "b@x.com", T0, T0, false);
        var r = DuplicidadeLeadResolver.resolver(tel("+5511988887777"), mail("b@x.com"), List.of(porMail));
        assertSame(porMail, r.orElseThrow());
    }

    @Test
    void ausenciaNaoCasa() {
        var semNada = lead(null, "+5511988887777", null, T0, T0, false);
        assertTrue(DuplicidadeLeadResolver.resolver(null, null, List.of(semNada)).isEmpty());
    }

    @Test
    void whatsappNaoEChave() {
        var l = lead("+5521999999999", "+5511988887777", "a@x.com", T0, T0, false);
        assertTrue(DuplicidadeLeadResolver.resolver(tel("+5511988887777"), mail("z@x.com"), List.of(l)).isEmpty());
    }

    @Test
    void desempatePorAtualizadoMaisRecente() {
        var antigo = lead("+5511988887777", null, null, T0, T0, false);
        var recente = lead("+5511988887777", null, null, T0, T0.plusSeconds(10), false);
        assertSame(recente, DuplicidadeLeadResolver.resolver(tel("+5511988887777"), null, List.of(antigo, recente)).orElseThrow());
    }

    @Test
    void empateUsaCriadoMaisAntigo() {
        var novo = lead("+5511988887777", null, null, T0.plusSeconds(5), T0.plusSeconds(10), false);
        var velho = lead("+5511988887777", null, null, T0, T0.plusSeconds(10), false);
        assertSame(velho, DuplicidadeLeadResolver.resolver(tel("+5511988887777"), null, List.of(novo, velho)).orElseThrow());
    }

    @Test
    void excluidoNaoEEscolhido() {
        var excl = lead("+5511988887777", null, null, T0, T0.plusSeconds(99), true);
        var ativo = lead("+5511988887777", null, null, T0, T0, false);
        assertSame(ativo, DuplicidadeLeadResolver.resolver(tel("+5511988887777"), null, List.of(excl, ativo)).orElseThrow());
    }
}
