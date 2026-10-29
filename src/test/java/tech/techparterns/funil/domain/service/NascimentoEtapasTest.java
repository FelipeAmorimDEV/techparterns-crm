package tech.techparterns.funil.domain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import tech.techparterns.funil.domain.model.CorEtapa;
import tech.techparterns.funil.domain.model.Etapa;
import tech.techparterns.funil.domain.model.OrdemEtapa;
import tech.techparterns.funil.domain.model.Probabilidade;
import tech.techparterns.funil.domain.model.SlaDias;
import tech.techparterns.funil.domain.model.TipoEtapa;
import tech.techparterns.shared.domain.CompanyId;

class NascimentoEtapasTest {

    @Test
    void empresaSemEtapasEPedidoNuloNasceAsTres() {
        var empresa = CompanyId.tryCreate("emp-1").getValue();
        var r = NascimentoEtapas.descrever(empresa, List.of(), null);
        assertTrue(r.ok());
        assertEquals(3, r.etapas().size());

        var aberto = unica(r.etapas(), TipoEtapa.ABERTO);
        assertTrue(aberto.ativa());
        assertEquals(0, aberto.probabilidade().valor());
        assertTrue(aberto.sla().vazio());
        assertNull(aberto.cor());
        assertFalse(aberto.exigeQualificacao());
        assertFalse(aberto.exigeDemo());
        assertEquals(1, aberto.ordem().valor());

        var ganho = unica(r.etapas(), TipoEtapa.GANHO);
        assertNascidaFinal(ganho, 2);

        var perdido = unica(r.etapas(), TipoEtapa.PERDIDO);
        assertNascidaFinal(perdido, 3);
    }

    @Test
    void adminCriandoAbertoValidoConservaOPedidoENasceOQueFalta() {
        var empresa = CompanyId.tryCreate("emp-1").getValue();
        var pedido = new NascimentoEtapas.Pedido(
                "Qualquer",
                TipoEtapa.ABERTO,
                OrdemEtapa.tryCreate(5).getValue(),
                true,
                Probabilidade.tryCreate(50).getValue(),
                CorEtapa.tryCreate("#1A2B3C").getValue(),
                SlaDias.tryCreate(2).getValue(),
                true,
                false);
        var r = NascimentoEtapas.descrever(empresa, List.of(), pedido);
        assertTrue(r.ok());
        assertEquals(3, r.etapas().size());
        assertEquals(1, r.etapas().stream().filter(e -> e.tipo() == TipoEtapa.ABERTO).count());

        var aberto = unica(r.etapas(), TipoEtapa.ABERTO);
        assertEquals("Qualquer", aberto.nome());
        assertEquals(TipoEtapa.ABERTO, aberto.tipo());
        assertEquals(5, aberto.ordem().valor());
        assertTrue(aberto.ativa());
        assertEquals(50, aberto.probabilidade().valor());
        assertEquals("#1A2B3C", aberto.cor().valor());
        assertEquals(2, aberto.sla().valor());
        assertTrue(aberto.exigeQualificacao());
        assertFalse(aberto.exigeDemo());

        assertNascidaFinal(unica(r.etapas(), TipoEtapa.GANHO), null);
        assertNascidaFinal(unica(r.etapas(), TipoEtapa.PERDIDO), null);
    }

    @Test
    void ordemRepetidaDevolveFalhaEListaVazia() {
        var empresa = CompanyId.tryCreate("emp-1").getValue();
        var existente = Etapa.tryCreate("emp-1", "Ja existe", TipoEtapa.ABERTO, 4, true, 0, null, null, false, false)
                .getValue();
        var pedido = new NascimentoEtapas.Pedido(
                "Outra",
                TipoEtapa.ABERTO,
                OrdemEtapa.tryCreate(4).getValue(),
                true,
                Probabilidade.tryCreate(10).getValue(),
                null,
                SlaDias.tryCreate(null).getValue(),
                false,
                false);
        var r = NascimentoEtapas.descrever(empresa, List.of(existente), pedido);
        assertFalse(r.ok());
        assertTrue(r.erros().contains(NascimentoEtapas.ORDEM_REPETIDA));
        assertTrue(r.etapas().isEmpty());
    }

    @Test
    void segundaGanhoAtivaNaoNasceOTipoQueFaltava() {
        var empresa = CompanyId.tryCreate("emp-1").getValue();
        var ganho = Etapa.nascida(empresa, TipoEtapa.GANHO, OrdemEtapa.tryCreate(2).getValue());
        var pedido = new NascimentoEtapas.Pedido(
                "Outra ganho",
                TipoEtapa.GANHO,
                OrdemEtapa.tryCreate(9).getValue(),
                true,
                null,
                null,
                SlaDias.tryCreate(null).getValue(),
                false,
                false);
        var r = NascimentoEtapas.descrever(empresa, List.of(ganho), pedido);
        assertFalse(r.ok());
        assertTrue(r.etapas().isEmpty());
        assertTrue(r.erros().contains(NascimentoEtapas.GANHO_ATIVA_EXISTE));
        assertEquals(0, r.etapas().stream().filter(e -> e.tipo() == TipoEtapa.PERDIDO).count());
    }

    @Test
    void segundaPerdidoAtivaNaoNasceOTipoQueFaltava() {
        var empresa = CompanyId.tryCreate("emp-1").getValue();
        var perdido = Etapa.nascida(empresa, TipoEtapa.PERDIDO, OrdemEtapa.tryCreate(3).getValue());
        var pedido = new NascimentoEtapas.Pedido(
                "Outra perdido",
                TipoEtapa.PERDIDO,
                OrdemEtapa.tryCreate(9).getValue(),
                true,
                null,
                null,
                SlaDias.tryCreate(null).getValue(),
                false,
                false);
        var r = NascimentoEtapas.descrever(empresa, List.of(perdido), pedido);
        assertFalse(r.ok());
        assertTrue(r.etapas().isEmpty());
        assertTrue(r.erros().contains(NascimentoEtapas.PERDIDO_ATIVA_EXISTE));
        assertEquals(0, r.etapas().stream().filter(e -> e.tipo() == TipoEtapa.GANHO).count());
    }

    private static void assertNascidaFinal(Etapa etapa, Integer ordem) {
        assertTrue(etapa.ativa());
        assertNull(etapa.probabilidade());
        assertTrue(etapa.sla().vazio());
        assertNull(etapa.cor());
        assertFalse(etapa.exigeQualificacao());
        assertFalse(etapa.exigeDemo());
        if (ordem != null)
            assertEquals(ordem, etapa.ordem().valor());
    }

    private static Etapa unica(List<Etapa> etapas, TipoEtapa tipo) {
        var encontradas = etapas.stream().filter(e -> e.tipo() == tipo).toList();
        assertEquals(1, encontradas.size());
        return encontradas.getFirst();
    }
}
