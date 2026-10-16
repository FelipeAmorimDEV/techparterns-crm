package tech.techparterns.distribuicao.domain.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import tech.techparterns.distribuicao.domain.model.RegraAtribuicao;

class MotorDistribuicaoTest {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");

    private static MotorDistribuicao.Vendedor v(String nome, int ordem, int peso, Integer limite, int abertos, boolean ativo,
                                                boolean participa) {
        var id = UUID.nameUUIDFromBytes(nome.getBytes());
        return new MotorDistribuicao.Vendedor(id, T0.plusSeconds(ordem), ativo, true, participa, peso, limite, abertos);
    }

    private static UUID id(String nome) {
        return UUID.nameUUIDFromBytes(nome.getBytes());
    }

    @Test
    void rodizioPercorreAeVoltaEmB() {
        var equipe = List.of(v("A", 1, 1, null, 0, true, true), v("B", 2, 1, null, 0, true, true),
                v("C", 3, 1, null, 0, true, true));
        Integer ponteiro = null;
        var recebidos = new ArrayList<UUID>();
        for (int i = 0; i < 4; i++) {
            var escolha = MotorDistribuicao.escolher(RegraAtribuicao.RODIZIO, null, null, equipe, List.of(), ponteiro);
            recebidos.add(escolha.vendedorId());
            ponteiro = escolha.ponteiro();
        }
        assertEquals(List.of(id("A"), id("B"), id("C"), id("A")), recebidos);
        var seguinte = MotorDistribuicao.escolher(RegraAtribuicao.RODIZIO, null, null, equipe, List.of(), ponteiro);
        assertEquals(id("B"), seguinte.vendedorId());
    }

    @Test
    void pesoDoisParaUmEPesoZeroFicaDeForaMasEntraNoRodizio() {
        var a = v("A", 1, 2, null, 0, true, true);
        var b = v("B", 2, 1, null, 0, true, true);
        var c = v("C", 3, 0, null, 0, true, true);
        var equipe = List.of(a, b, c);
        Integer ponteiro = null;
        var recebidos = new ArrayList<UUID>();
        for (int i = 0; i < 3; i++) {
            var escolha = MotorDistribuicao.escolher(RegraAtribuicao.PESO, null, null, equipe, List.of(), ponteiro);
            recebidos.add(escolha.vendedorId());
            ponteiro = escolha.ponteiro();
        }
        assertEquals(List.of(id("A"), id("A"), id("B")), recebidos);
        var rodizio = new ArrayList<UUID>();
        Integer p = null;
        for (int i = 0; i < 3; i++) {
            var escolha = MotorDistribuicao.escolher(RegraAtribuicao.RODIZIO, null, null, equipe, List.of(), p);
            rodizio.add(escolha.vendedorId());
            p = escolha.ponteiro();
        }
        assertTrue(rodizio.contains(id("C")));
    }

    @Test
    void cargaVaiParaQuemTemMenosEEmpateUsaRodizio() {
        var a = v("A", 1, 1, null, 1, true, true);
        var b = v("B", 2, 1, null, 0, true, true);
        var unica = MotorDistribuicao.escolher(RegraAtribuicao.CARGA, null, null, List.of(a, b), List.of(), null);
        assertEquals(id("B"), unica.vendedorId());
        assertNull(unica.ponteiro());

        var empatados = List.of(a.comAbertos(0), b);
        var primeiro = MotorDistribuicao.escolher(RegraAtribuicao.CARGA, null, null, empatados, List.of(), null);
        assertEquals(id("A"), primeiro.vendedorId());
        var segundo = MotorDistribuicao.escolher(RegraAtribuicao.CARGA, null, null, empatados, List.of(), primeiro.ponteiro());
        assertEquals(id("B"), segundo.vendedorId());
    }

    @Test
    void territorioDeCidadeGanhaDaUfEInelegivelCaiNoRodizio() {
        var cidade = v("Cidade", 2, 1, null, 0, true, true);
        var uf = v("Uf", 3, 1, null, 0, true, true);
        var outro = v("Outro", 1, 1, null, 0, true, true);
        var territorios = List.of(
                new MotorDistribuicao.TerritorioRef("SP", "São Paulo", cidade.id()),
                new MotorDistribuicao.TerritorioRef("SP", null, uf.id()));
        var escolha = MotorDistribuicao.escolher(RegraAtribuicao.TERRITORIO, "sp", " São Paulo ",
                List.of(cidade, uf, outro), territorios, null);
        assertEquals(cidade.id(), escolha.vendedorId());
        assertEquals(RegraAtribuicao.TERRITORIO, escolha.regra());
        assertNull(escolha.ponteiro());

        var inativo = v("Cidade", 2, 1, null, 0, false, true);
        var caiu = MotorDistribuicao.escolher(RegraAtribuicao.TERRITORIO, "SP", "São Paulo",
                List.of(inativo, uf, outro), territorios, null);
        assertEquals(RegraAtribuicao.RODIZIO, caiu.regra());
        assertEquals(outro.id(), caiu.vendedorId());
        assertNotEquals(uf.id(), caiu.vendedorId());
    }

    @Test
    void cidadeComAcentoDiferenteNaoCasaEUfIgnoraCaixa() {
        assertTrue(MotorDistribuicao.cidadesIguais(" São Paulo ", "São Paulo"));
        assertFalse(MotorDistribuicao.cidadesIguais("Sao Paulo", "São Paulo"));
        assertFalse(MotorDistribuicao.cidadesIguais("são paulo", "São Paulo"));
        assertTrue(MotorDistribuicao.ufsIguais("sp", "SP"));
    }

    @Test
    void ordemDaFilaColocaQuenteAntesEDevolvidoNaoQuenteNoFim() {
        record Item(String nome, Instant criado, boolean quente, boolean devolvido) {}
        var itens = new ArrayList<>(List.of(
                new Item("frio-devolvido", T0.plusSeconds(1), false, true),
                new Item("quente-novo", T0.plusSeconds(5), true, false),
                new Item("quente-velho-devolvido", T0.plusSeconds(2), true, true),
                new Item("frio-comum", T0, false, false)));
        itens.sort(Comparator
                .comparingInt((Item i) -> MotorDistribuicao.grupoFila(i.quente, i.devolvido))
                .thenComparing(Item::criado));
        assertEquals(List.of("quente-velho-devolvido", "quente-novo", "frio-comum", "frio-devolvido"),
                itens.stream().map(Item::nome).toList());
    }

    @Test
    void inelegivelNaoRecebePorRegra() {
        var fora = v("Fora", 1, 1, 0, 0, true, true);
        var parado = v("Parado", 2, 1, null, 0, true, false);
        assertNull(MotorDistribuicao.escolher(RegraAtribuicao.RODIZIO, null, null, List.of(fora, parado), List.of(), null)
                .vendedorId());
        assertNull(MotorDistribuicao.escolher(RegraAtribuicao.PESO, null, null, List.of(fora), List.of(), null).vendedorId());
        assertNull(MotorDistribuicao.escolher(RegraAtribuicao.CARGA, null, null, List.of(parado), List.of(), null).vendedorId());
    }
}
