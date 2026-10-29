package tech.techparterns.funil.application;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import tech.techparterns.funil.application.command.ColocarLeadsJaGravadosCommand;
import tech.techparterns.funil.application.usecase.ColocarLeadsJaGravadosUseCase;
import tech.techparterns.funil.domain.model.Etapa;
import tech.techparterns.funil.domain.model.LeadGanho;
import tech.techparterns.funil.domain.model.LeadMudouEtapa;
import tech.techparterns.funil.domain.model.LeadPerdido;
import tech.techparterns.funil.domain.model.NegocioFunil;
import tech.techparterns.funil.domain.model.TipoEtapa;
import tech.techparterns.funil.domain.model.ValorComercial;
import tech.techparterns.funil.domain.repository.EtapaRepository;
import tech.techparterns.funil.domain.repository.NegocioFunilRepository;
import tech.techparterns.funil.domain.service.ValorColuna;
import tech.techparterns.funil.infrastructure.persistence.HistoricoEtapaEntity;
import tech.techparterns.lead.domain.model.Lead;
import tech.techparterns.lead.domain.model.NomeContato;
import tech.techparterns.lead.domain.model.OrigemLead;
import tech.techparterns.lead.domain.model.SituacaoLead;
import tech.techparterns.lead.domain.model.TelefoneBrasil;
import tech.techparterns.lead.domain.repository.LeadRepository;
import tech.techparterns.shared.domain.CompanyId;
import tech.techparterns.shared.domain.Result;

@QuarkusTest
class ColocarLeadsJaGravadosUseCaseTest {

    @Inject
    ColocarLeadsJaGravadosUseCase colocar;

    @Inject
    LeadRepository leads;

    @Inject
    EtapaRepository etapas;

    @Inject
    NegocioFunilRepository negocios;

    @Inject
    CapturaEventosFunil captura;

    @Inject
    EntityManager em;

    @Test
    void leadAbertaNaoExcluidoEntraNaAbertoAtivaDeMenorOrdem() {
        var empresa = CompanyId.reconstituir("funil-t5-aberta");
        esvaziarPendencias();
        captura.limpar();
        gravarEtapa(empresa.valor(), "Coluna um", TipoEtapa.ABERTO, 1);
        gravarEtapa(empresa.valor(), "Final ganho", TipoEtapa.GANHO, 2);
        gravarEtapa(empresa.valor(), "Final perdido", TipoEtapa.PERDIDO, 3);
        var lead = gravarAberta(empresa, "Ana", "+5511900005101");

        var resultado = colocar.execute(new ColocarLeadsJaGravadosCommand());

        assertTrue(resultado.isOk());
        var aberto = abertoAtivaDeMenorOrdem(etapas.listarPorEmpresa(empresa));
        var negocio = negocios.buscar(lead.getId(), empresa).orElseThrow();
        assertEquals(aberto.getId(), negocio.etapaId());
        assertHistoricoDeEntrada(aberto.getId());
        assertEquals(1, captura.ocorrencias(LeadMudouEtapa.class));
        assertEquals(0, captura.ocorrencias(LeadGanho.class));
        assertEquals(0, captura.ocorrencias(LeadPerdido.class));
    }

    @Test
    void duasAbertoAtivasLeadEntraNaDeOrdemUm() {
        var empresa = CompanyId.reconstituir("funil-t5-ordens");
        esvaziarPendencias();
        gravarEtapa(empresa.valor(), "Coluna um", TipoEtapa.ABERTO, 1);
        gravarEtapa(empresa.valor(), "Coluna dois", TipoEtapa.ABERTO, 2);
        gravarEtapa(empresa.valor(), "Final ganho", TipoEtapa.GANHO, 3);
        gravarEtapa(empresa.valor(), "Final perdido", TipoEtapa.PERDIDO, 4);
        var lead = gravarAberta(empresa, "Bia", "+5511900005102");

        assertTrue(colocar.execute(new ColocarLeadsJaGravadosCommand()).isOk());

        var lista = etapas.listarPorEmpresa(empresa);
        var ordem1 = porOrdem(lista, 1);
        var ordem2 = porOrdem(lista, 2);
        assertEquals(TipoEtapa.ABERTO, ordem1.tipo());
        assertEquals(TipoEtapa.ABERTO, ordem2.tipo());
        assertTrue(ordem1.ativa());
        assertTrue(ordem2.ativa());
        var negocio = negocios.buscar(lead.getId(), empresa).orElseThrow();
        assertEquals(ordem1.getId(), negocio.etapaId());
        assertNotEquals(ordem2.getId(), negocio.etapaId());
    }

    @Test
    void leadGanhoEPerdidoEntramNasFinaisSemEventoDeConclusao() {
        var empresa = CompanyId.reconstituir("funil-t5-finais");
        esvaziarPendencias();
        captura.limpar();
        gravarEtapa(empresa.valor(), "Coluna um", TipoEtapa.ABERTO, 1);
        gravarEtapa(empresa.valor(), "Final ganho", TipoEtapa.GANHO, 2);
        gravarEtapa(empresa.valor(), "Final perdido", TipoEtapa.PERDIDO, 3);
        var ganho = gravarSituacao(empresa, "Carla", "+5511900005103", SituacaoLead.GANHO, null);
        var perdido = gravarSituacao(empresa, "Dora", "+5511900005104", SituacaoLead.PERDIDO, null);

        assertTrue(colocar.execute(new ColocarLeadsJaGravadosCommand()).isOk());

        var lista = etapas.listarPorEmpresa(empresa);
        var etapaGanho = unica(lista, TipoEtapa.GANHO);
        var etapaPerdido = unica(lista, TipoEtapa.PERDIDO);
        assertTrue(etapaGanho.ativa());
        assertTrue(etapaPerdido.ativa());
        assertEquals(etapaGanho.getId(), negocios.buscar(ganho.getId(), empresa).orElseThrow().etapaId());
        assertEquals(etapaPerdido.getId(), negocios.buscar(perdido.getId(), empresa).orElseThrow().etapaId());
        assertHistoricoDeEntrada(etapaGanho.getId());
        assertHistoricoDeEntrada(etapaPerdido.getId());
        assertEquals(0, captura.ocorrencias(LeadGanho.class));
        assertEquals(0, captura.ocorrencias(LeadPerdido.class));
        assertEquals(0, captura.ocorrencias(LeadMudouEtapa.class));
    }

    @Test
    void leadExcluidoNaoGanhaLinha() {
        var empresa = CompanyId.reconstituir("funil-t5-excluido");
        esvaziarPendencias();
        var lead = gravarSituacao(empresa, "Eva", "+5511900005105", SituacaoLead.ABERTA,
                OffsetDateTime.parse("2026-04-03T00:00:00Z"));

        assertTrue(colocar.execute(new ColocarLeadsJaGravadosCommand()).isOk());

        assertTrue(negocios.buscar(lead.getId(), empresa).isEmpty());
        assertEquals(0, contarNegocios(lead.getId()));
    }

    @Test
    void empresaSemEtapasNasceEColocaNaAbertoDeMenorOrdem() {
        var empresa = CompanyId.reconstituir("funil-t5-nascidas");
        esvaziarPendencias();
        var lead = gravarAberta(empresa, "Fabi", "+5511900005106");

        assertTrue(colocar.execute(new ColocarLeadsJaGravadosCommand()).isOk());

        var lista = etapas.listarPorEmpresa(empresa);
        assertEquals(3, lista.size());
        var aberto = unica(lista, TipoEtapa.ABERTO);
        assertNascidaAberta(aberto);
        assertNascidaFinal(unica(lista, TipoEtapa.GANHO));
        assertNascidaFinal(unica(lista, TipoEtapa.PERDIDO));
        assertEquals(aberto.getId(), abertoAtivaDeMenorOrdem(lista).getId());
        assertEquals(aberto.getId(), negocios.buscar(lead.getId(), empresa).orElseThrow().etapaId());
    }

    @Test
    void segundaExecucaoNaoAcrescentaLinhaHistoricoNemEvento() {
        var empresa = CompanyId.reconstituir("funil-t5-idempotente");
        esvaziarPendencias();
        captura.limpar();
        gravarEtapa(empresa.valor(), "Coluna um", TipoEtapa.ABERTO, 1);
        gravarEtapa(empresa.valor(), "Final ganho", TipoEtapa.GANHO, 2);
        gravarEtapa(empresa.valor(), "Final perdido", TipoEtapa.PERDIDO, 3);
        var lead = gravarAberta(empresa, "Gabi", "+5511900005107");

        assertTrue(colocar.execute(new ColocarLeadsJaGravadosCommand()).isOk());
        var negocio = negocios.buscar(lead.getId(), empresa).orElseThrow();
        assertEquals(1, contarNegocios(lead.getId()));
        assertEquals(1, historico(negocio.etapaId()).size());

        captura.limpar();
        assertTrue(colocar.execute(new ColocarLeadsJaGravadosCommand()).isOk());

        var deNovo = negocios.buscar(lead.getId(), empresa).orElseThrow();
        assertEquals(negocio.getId(), deNovo.getId());
        assertEquals(negocio.etapaId(), deNovo.etapaId());
        assertEquals(1, contarNegocios(lead.getId()));
        assertEquals(1, historico(negocio.etapaId()).size());
        assertEquals(0, captura.ocorrencias(LeadMudouEtapa.class));
        assertEquals(0, captura.ocorrencias(LeadGanho.class));
        assertEquals(0, captura.ocorrencias(LeadPerdido.class));
    }

    @Test
    void doisGanhosUmComDezEOutroNuloSomamDezEmDuasPosicoes() {
        var empresa = CompanyId.reconstituir("funil-t5-soma");
        esvaziarPendencias();
        gravarEtapa(empresa.valor(), "Coluna um", TipoEtapa.ABERTO, 1);
        var ganho = gravarEtapa(empresa.valor(), "Final ganho", TipoEtapa.GANHO, 2);
        gravarEtapa(empresa.valor(), "Final perdido", TipoEtapa.PERDIDO, 3);
        var comValor = gravarSituacao(empresa, "Helena", "+5511900005108", SituacaoLead.GANHO, null);
        var semLinha = gravarSituacao(empresa, "Iris", "+5511900005109", SituacaoLead.GANHO, null);
        var dez = ValorComercial.tryCreate(new BigDecimal("10.00"));
        assertTrue(dez.isOk());
        var agora = Instant.now();
        negocios.salvar(NegocioFunil.reconstituir(
                UUID.randomUUID(), agora, agora, null,
                comValor.getId(), ganho.getId(), agora,
                null, null, null, null, null, null,
                null, null, null, null, dez.getValue(), null, null));

        assertTrue(colocar.execute(new ColocarLeadsJaGravadosCommand()).isOk());

        var ja = negocios.buscar(comValor.getId(), empresa).orElseThrow();
        var colocado = negocios.buscar(semLinha.getId(), empresa).orElseThrow();
        assertEquals(ganho.getId(), ja.etapaId());
        assertEquals(ganho.getId(), colocado.etapaId());
        assertNull(colocado.valorFechado());
        assertEquals(new BigDecimal("10.00"), ja.valorFechado().valor());
        var etapa = unica(etapas.listarPorEmpresa(empresa), TipoEtapa.GANHO);
        var fechado = ValorColuna.contribuicao(
                TipoEtapa.GANHO, ja.valorEstimado(), ja.valorFechado(), etapa.probabilidade());
        var vazio = ValorColuna.contribuicao(
                TipoEtapa.GANHO, colocado.valorEstimado(), colocado.valorFechado(), etapa.probabilidade());
        assertEquals(new BigDecimal("10.00"), fechado.valor().add(vazio.valor()));
        assertEquals(2, contarNegociosDaEmpresa(empresa.valor()));
    }

    /**
     * A subida com banco vazio já ocorreu: este {@code @QuarkusTest} só chega aqui
     * se {@code ColocacaoFunilNaSubida} não derrubar o processo.
     */
    @Test
    void executeComNadaPendenteDevolveSucesso() {
        assertDoesNotThrow(() -> {
            var drenado = colocar.execute(new ColocarLeadsJaGravadosCommand());
            assertTrue(drenado.isOk());
            var resultado = colocar.execute(new ColocarLeadsJaGravadosCommand());
            assertTrue(resultado.isOk());
            assertFalse(resultado.isFailure());
        });
    }

    @Test
    void falhaDeUmLeadNaoApagaLinhaDoOutro() {
        var empresa = CompanyId.reconstituir("funil-t5-falha");
        esvaziarPendencias();
        gravarEtapa(empresa.valor(), "Coluna um", TipoEtapa.ABERTO, 1);
        gravarEtapa(empresa.valor(), "Final ganho", TipoEtapa.GANHO, 2);
        gravarEtapa(empresa.valor(), "Final perdido", TipoEtapa.PERDIDO, 3);
        var aberta = gravarAberta(empresa, "Julia", "+5511900005110");
        var invalido = gravarAberta(empresa, "Kaia", "+5511900005111");
        // V5 declara CHECK (ABERTA, GANHO, PERDIDO). O cenário pede valor fora dessa lista.
        suspenderCheckSituacao();
        try {
            marcarSituacao(invalido.getId(), "FORA");

            var resultado = assertDoesNotThrow(() -> colocar.execute(new ColocarLeadsJaGravadosCommand()));

            assertTrue(resultado.isOk());
            var linha = negocios.buscar(aberta.getId(), empresa);
            assertTrue(linha.isPresent());
            assertTrue(negocios.buscar(invalido.getId(), empresa).isEmpty());
            assertEquals(linha.orElseThrow().getId(), negocios.buscar(aberta.getId(), empresa).orElseThrow().getId());
            assertEquals(1, contarNegocios(aberta.getId()));
            assertEquals(0, contarNegocios(invalido.getId()));
        } finally {
            marcarSituacao(invalido.getId(), "ABERTA");
            restaurarCheckSituacao();
        }
    }

    private Result<Void> esvaziarPendencias() {
        var resultado = colocar.execute(new ColocarLeadsJaGravadosCommand());
        assertTrue(resultado.isOk());
        return resultado;
    }

    private Lead gravarAberta(CompanyId empresa, String nome, String telefone) {
        var criado = Lead.tryCreate(empresa, NomeContato.reconstituir(nome),
                TelefoneBrasil.reconstituir(telefone), null, null);
        assertTrue(criado.isOk());
        leads.salvar(criado.getValue());
        return criado.getValue();
    }

    private Lead gravarSituacao(CompanyId empresa, String nome, String telefone, SituacaoLead situacao,
                                OffsetDateTime deletedAt) {
        var agora = Instant.now();
        var lead = Lead.reconstituir(UUID.randomUUID(), agora, agora, deletedAt, empresa,
                NomeContato.reconstituir(nome), TelefoneBrasil.reconstituir(telefone), null, null,
                OrigemLead.IMPORTACAO, situacao, null, null, false);
        leads.salvar(lead);
        return lead;
    }

    private Etapa gravarEtapa(String empresa, String nome, TipoEtapa tipo, int ordem) {
        var criada = Etapa.tryCreate(empresa, nome, tipo, ordem, true, null, null, null, false, false);
        assertTrue(criada.isOk());
        etapas.salvar(criada.getValue());
        return criada.getValue();
    }

    private void suspenderCheckSituacao() {
        QuarkusTransaction.requiringNew().run(() -> em.createNativeQuery(
                "alter table lead drop constraint if exists lead_situacao_check").executeUpdate());
    }

    private void restaurarCheckSituacao() {
        QuarkusTransaction.requiringNew().run(() -> em.createNativeQuery("""
                alter table lead add constraint lead_situacao_check
                check (situacao in ('ABERTA', 'GANHO', 'PERDIDO'))
                """).executeUpdate());
    }

    private void marcarSituacao(UUID leadId, String situacao) {
        QuarkusTransaction.requiringNew().run(() -> {
            var atualizados = em.createNativeQuery("update lead set situacao = :situacao where id = :id")
                    .setParameter("situacao", situacao)
                    .setParameter("id", leadId)
                    .executeUpdate();
            assertEquals(1, atualizados);
            em.clear();
        });
    }

    private long contarNegocios(UUID leadId) {
        return QuarkusTransaction.requiringNew().call(() -> {
            em.clear();
            return em.createQuery("select count(n) from NegocioFunilEntity n where n.leadId = :leadId", Long.class)
                    .setParameter("leadId", leadId)
                    .getSingleResult();
        });
    }

    private long contarNegociosDaEmpresa(String empresa) {
        return QuarkusTransaction.requiringNew().call(() -> {
            em.clear();
            return em.createQuery("""
                            select count(n) from NegocioFunilEntity n
                            where n.leadId in (select l.id from LeadEntity l where l.companyId = :empresa)
                            """, Long.class)
                    .setParameter("empresa", empresa)
                    .getSingleResult();
        });
    }

    private List<HistoricoEtapaEntity> historico(UUID destino) {
        return QuarkusTransaction.requiringNew().call(() -> {
            em.clear();
            return em.createQuery(
                            "from HistoricoEtapaEntity h where h.destinoEtapaId = :destino",
                            HistoricoEtapaEntity.class)
                    .setParameter("destino", destino)
                    .getResultList();
        });
    }

    private void assertHistoricoDeEntrada(UUID destino) {
        var linhas = historico(destino);
        assertEquals(1, linhas.size());
        assertNull(linhas.get(0).origemEtapaId);
        assertEquals(destino, linhas.get(0).destinoEtapaId);
        assertNull(linhas.get(0).userId);
    }

    private static void assertNascidaAberta(Etapa etapa) {
        assertEquals(TipoEtapa.ABERTO, etapa.tipo());
        assertTrue(etapa.ativa());
        assertNotNull(etapa.probabilidade());
        assertEquals(0, etapa.probabilidade().valor());
        assertTrue(etapa.sla().vazio());
        assertNull(etapa.cor());
        assertFalse(etapa.exigeQualificacao());
        assertFalse(etapa.exigeDemo());
    }

    private static void assertNascidaFinal(Etapa etapa) {
        assertTrue(etapa.ativa());
        assertNull(etapa.probabilidade());
        assertTrue(etapa.sla().vazio());
        assertNull(etapa.cor());
        assertFalse(etapa.exigeQualificacao());
        assertFalse(etapa.exigeDemo());
    }

    private static Etapa unica(List<Etapa> lista, TipoEtapa tipo) {
        var encontradas = lista.stream().filter(e -> e.tipo() == tipo).toList();
        assertEquals(1, encontradas.size(), tipo.name());
        return encontradas.get(0);
    }

    private static Etapa porOrdem(List<Etapa> lista, int ordem) {
        var encontradas = lista.stream().filter(e -> e.ordem().valor() == ordem).toList();
        assertEquals(1, encontradas.size());
        return encontradas.get(0);
    }

    private static Etapa abertoAtivaDeMenorOrdem(List<Etapa> lista) {
        return lista.stream()
                .filter(etapa -> etapa.tipo() == TipoEtapa.ABERTO && etapa.ativa())
                .min(Comparator.comparingInt(etapa -> etapa.ordem().valor()))
                .orElseThrow();
    }
}
