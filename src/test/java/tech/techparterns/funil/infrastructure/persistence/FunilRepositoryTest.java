package tech.techparterns.funil.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import tech.techparterns.distribuicao.domain.model.DonoLead;
import tech.techparterns.distribuicao.domain.repository.DistribuicaoRepository;
import tech.techparterns.funil.domain.model.Etapa;
import tech.techparterns.funil.domain.model.HistoricoEtapa;
import tech.techparterns.funil.domain.model.MotivoPerda;
import tech.techparterns.funil.domain.model.NegocioFunil;
import tech.techparterns.funil.domain.model.TipoEtapa;
import tech.techparterns.funil.domain.port.ConsultaLeadFunil;
import tech.techparterns.funil.domain.repository.EtapaRepository;
import tech.techparterns.funil.domain.repository.HistoricoEtapaRepository;
import tech.techparterns.funil.domain.repository.MotivoPerdaRepository;
import tech.techparterns.funil.domain.repository.NegocioFunilRepository;
import tech.techparterns.identidade.infrastructure.persistence.EmpresaEntity;
import tech.techparterns.identidade.infrastructure.persistence.UsuarioEmpresaEntity;
import tech.techparterns.lead.domain.model.Lead;
import tech.techparterns.lead.domain.model.NomeContato;
import tech.techparterns.lead.domain.model.SituacaoLead;
import tech.techparterns.lead.domain.model.TelefoneBrasil;
import tech.techparterns.lead.domain.repository.LeadRepository;
import tech.techparterns.shared.domain.CompanyId;

@QuarkusTest
class FunilRepositoryTest {

    private static final Instant ENTRADA = Instant.parse("2026-04-01T12:00:00Z");
    private static final Instant TRANSICAO = Instant.parse("2026-04-01T15:30:00Z");

    @Inject
    EtapaRepository etapas;
    @Inject
    MotivoPerdaRepository motivos;
    @Inject
    NegocioFunilRepository negocios;
    @Inject
    HistoricoEtapaRepository historicos;
    @Inject
    ConsultaLeadFunil consulta;
    @Inject
    LeadRepository leads;
    @Inject
    DistribuicaoRepository distribuicao;
    @Inject
    EntityManager em;

    @Test
    void gravaEReleDaMesmaEmpresa() {
        var empresa = CompanyId.reconstituir("funil-t2-grava");
        var lead = gravarLead(empresa, "+5511900001101");
        var etapa = gravarEtapa(empresa.valor(), "Contato", TipoEtapa.ABERTO, 1);
        var motivo = gravarMotivo(empresa.valor(), "Preco", true);
        var negocio = gravarNegocio(lead.getId(), etapa.getId(), ENTRADA);
        var usuario = UUID.randomUUID();
        var historico = gravarHistorico(null, etapa.getId(), usuario, TRANSICAO);

        var etapaLida = etapas.buscarPorId(etapa.getId(), empresa).orElseThrow();
        assertEquals(empresa.valor(), etapaLida.companyId().valor());
        assertEquals("Contato", etapaLida.nome());
        assertEquals(TipoEtapa.ABERTO, etapaLida.tipo());
        assertEquals(1, etapaLida.ordem().valor());
        assertTrue(etapaLida.ativa());
        assertNull(etapaLida.probabilidade());
        assertNull(etapaLida.cor());
        assertTrue(etapaLida.sla().vazio());
        assertFalse(etapaLida.exigeQualificacao());
        assertFalse(etapaLida.exigeDemo());

        var motivoLido = motivos.buscarPorId(motivo.getId(), empresa).orElseThrow();
        assertEquals(empresa.valor(), motivoLido.companyId().valor());
        assertEquals("Preco", motivoLido.nome());
        assertTrue(motivoLido.ativo());

        var negocioLido = negocios.buscar(lead.getId(), empresa).orElseThrow();
        assertEquals(negocio.getId(), negocioLido.getId());
        assertEquals(lead.getId(), negocioLido.leadId());
        assertEquals(etapa.getId(), negocioLido.etapaId());
        assertEquals(ENTRADA.toEpochMilli(), negocioLido.entrouNaEtapa().toEpochMilli());

        var historicoLido = historicos.buscarPorId(historico.getId(), empresa).orElseThrow();
        assertEquals(historico.getId(), historicoLido.getId());
        assertNull(historicoLido.origemEtapaId());
        assertEquals(etapa.getId(), historicoLido.destinoEtapaId());
        assertEquals(usuario, historicoLido.userId());
        assertEquals(TRANSICAO.toEpochMilli(), historicoLido.quando().toEpochMilli());
    }

    @Test
    void segundoNegocioDoMesmoLeadFalha() {
        var empresa = CompanyId.reconstituir("funil-t2-lead");
        var lead = gravarLead(empresa, "+5511900001201");
        var primeira = gravarEtapa(empresa.valor(), "Contato", TipoEtapa.ABERTO, 1);
        var segunda = gravarEtapa(empresa.valor(), "Proposta", TipoEtapa.ABERTO, 2);
        gravarNegocio(lead.getId(), primeira.getId(), ENTRADA);

        var repetido = NegocioFunil.tryCreate(lead.getId(), segunda.getId(), ENTRADA).getValue();
        assertViolacaoUnica(() -> negocios.salvar(repetido));

        var lido = negocios.buscar(lead.getId(), empresa).orElseThrow();
        assertEquals(primeira.getId(), lido.etapaId());
    }

    @Test
    void segundaEtapaComMesmaOrdemFalha() {
        var empresa = CompanyId.reconstituir("funil-t2-ordem");
        var primeira = gravarEtapa(empresa.valor(), "Contato", TipoEtapa.ABERTO, 1);
        var repetida = Etapa.tryCreate(empresa.valor(), "Proposta", TipoEtapa.ABERTO, 1, true,
                null, null, null, false, false).getValue();

        assertViolacaoUnica(() -> etapas.salvar(repetida));

        assertEquals("Contato", etapas.buscarPorId(primeira.getId(), empresa).orElseThrow().nome());
        assertTrue(etapas.buscarPorId(repetida.getId(), empresa).isEmpty());
    }

    @Test
    void segundaGanhoAtivaFalha() {
        var empresa = CompanyId.reconstituir("funil-t2-ganho");
        var primeira = gravarEtapa(empresa.valor(), "Ganho", TipoEtapa.GANHO, 1);
        var repetida = Etapa.tryCreate(empresa.valor(), "Ganho extra", TipoEtapa.GANHO, 2, true,
                null, null, null, false, false).getValue();

        assertViolacaoUnica(() -> etapas.salvar(repetida));

        assertEquals(TipoEtapa.GANHO, etapas.buscarPorId(primeira.getId(), empresa).orElseThrow().tipo());
        assertTrue(etapas.buscarPorId(repetida.getId(), empresa).isEmpty());
    }

    @Test
    void segundaPerdidoAtivaFalha() {
        var empresa = CompanyId.reconstituir("funil-t2-perdido");
        var primeira = gravarEtapa(empresa.valor(), "Perdido", TipoEtapa.PERDIDO, 1);
        var repetida = Etapa.tryCreate(empresa.valor(), "Perdido extra", TipoEtapa.PERDIDO, 2, true,
                null, null, null, false, false).getValue();

        assertViolacaoUnica(() -> etapas.salvar(repetida));

        assertEquals(TipoEtapa.PERDIDO, etapas.buscarPorId(primeira.getId(), empresa).orElseThrow().tipo());
        assertTrue(etapas.buscarPorId(repetida.getId(), empresa).isEmpty());
    }

    @Test
    void buscaFiltraCompanyId() {
        var a = CompanyId.reconstituir("funil-t2-filtro-a");
        var b = CompanyId.reconstituir("funil-t2-filtro-b");
        var lead = gravarLead(a, "+5511900001601");
        var etapa = gravarEtapa(a.valor(), "Contato", TipoEtapa.ABERTO, 1);
        var motivo = gravarMotivo(a.valor(), "Preco", true);
        gravarNegocio(lead.getId(), etapa.getId(), ENTRADA);
        var historico = gravarHistorico(null, etapa.getId(), null, TRANSICAO);

        assertTrue(etapas.buscarPorId(etapa.getId(), b).isEmpty());
        assertTrue(motivos.buscarPorId(motivo.getId(), b).isEmpty());
        assertTrue(negocios.buscar(lead.getId(), b).isEmpty());
        assertTrue(historicos.buscarPorId(historico.getId(), b).isEmpty());

        assertTrue(etapas.buscarPorId(etapa.getId(), a).isPresent());
        assertTrue(motivos.buscarPorId(motivo.getId(), a).isPresent());
        assertTrue(negocios.buscar(lead.getId(), a).isPresent());
        assertTrue(historicos.buscarPorId(historico.getId(), a).isPresent());
    }

    @Test
    void historicoNaoOfereceExclusao() {
        for (var metodo : HistoricoEtapaRepository.class.getMethods()) {
            var nome = metodo.getName().toLowerCase(Locale.ROOT);
            assertFalse(nome.contains("excluir") || nome.contains("remover")
                    || nome.contains("delete") || nome.contains("apagar"), metodo.getName());
        }
    }

    @Test
    void consultaLeadDevolveFatosEDonoNuloSemPosse() {
        var empresa = CompanyId.reconstituir("funil-t2-consulta");
        var aberto = leadComFatos(empresa, "+5511900001701", SituacaoLead.GANHO, true, "SP", "Campinas", false);
        var excluido = leadComFatos(empresa, "+5511900001702", SituacaoLead.PERDIDO, false, "RJ", "Niteroi", true);

        var fato = consulta.buscar(empresa, aberto.getId()).orElseThrow();
        assertEquals("GANHO", fato.situacao());
        assertEquals("SP", fato.uf());
        assertEquals("Campinas", fato.cidade());
        assertTrue(fato.quente());
        assertFalse(fato.exclusao());
        assertNull(fato.dono());

        var fatoExcluido = consulta.buscar(empresa, excluido.getId()).orElseThrow();
        assertEquals("PERDIDO", fatoExcluido.situacao());
        assertEquals("RJ", fatoExcluido.uf());
        assertEquals("Niteroi", fatoExcluido.cidade());
        assertFalse(fatoExcluido.quente());
        assertTrue(fatoExcluido.exclusao());
        assertNull(fatoExcluido.dono());

        var outra = CompanyId.reconstituir("funil-t2-consulta-b");
        assertTrue(consulta.buscar(outra, aberto.getId()).isEmpty());
        assertTrue(consulta.buscar(outra, excluido.getId()).isEmpty());
    }

    @Test
    void consultaLeadDevolveDonoPreenchido() {
        var companyRow = UUID.randomUUID();
        var usuario = UUID.randomUUID();
        var empresa = CompanyId.reconstituir("funil-t2-dono-" + companyRow);
        criarUsuario(companyRow, usuario);
        var lead = leadComFatos(empresa, "+5511900001801", SituacaoLead.ABERTA, true, "MG", "Uberlandia", false);
        distribuicao.salvarDono(
                new DonoLead(lead.getId(), empresa.valor(), usuario, ENTRADA, false), ENTRADA);

        var fato = consulta.buscar(empresa, lead.getId()).orElseThrow();
        assertEquals("ABERTA", fato.situacao());
        assertEquals("MG", fato.uf());
        assertEquals("Uberlandia", fato.cidade());
        assertTrue(fato.quente());
        assertFalse(fato.exclusao());
        assertEquals(usuario, fato.dono());
        assertTrue(consulta.buscar(CompanyId.reconstituir("funil-t2-dono-outra"), lead.getId()).isEmpty());
    }

    private Lead gravarLead(CompanyId empresa, String telefone) {
        var lead = Lead.tryCreate(empresa, NomeContato.reconstituir("Ana"),
                TelefoneBrasil.reconstituir(telefone), null, null).getValue();
        leads.salvar(lead);
        return lead;
    }

    private Lead leadComFatos(CompanyId empresa, String telefone, SituacaoLead situacao, boolean quente,
                              String uf, String cidade, boolean excluir) {
        var criado = Lead.tryCreate(empresa, NomeContato.reconstituir("Ana"),
                TelefoneBrasil.reconstituir(telefone), null, null).getValue();
        var lead = criado.comFatos(situacao, quente, uf, cidade, ENTRADA).getValue();
        if (excluir)
            lead.excluirLogicamente(OffsetDateTime.parse("2026-04-03T00:00:00Z"));
        leads.salvar(lead);
        return lead;
    }

    private Etapa gravarEtapa(String companyId, String nome, TipoEtapa tipo, int ordem) {
        var etapa = Etapa.tryCreate(companyId, nome, tipo, ordem, true, null, null, null, false, false).getValue();
        etapas.salvar(etapa);
        return etapa;
    }

    private MotivoPerda gravarMotivo(String companyId, String nome, boolean ativo) {
        var motivo = MotivoPerda.tryCreate(companyId, nome, ativo).getValue();
        motivos.salvar(motivo);
        return motivo;
    }

    private NegocioFunil gravarNegocio(UUID leadId, UUID etapaId, Instant entrouNaEtapa) {
        var negocio = NegocioFunil.tryCreate(leadId, etapaId, entrouNaEtapa).getValue();
        negocios.salvar(negocio);
        return negocio;
    }

    private HistoricoEtapa gravarHistorico(UUID origem, UUID destino, UUID userId, Instant quando) {
        var historico = HistoricoEtapa.tryCreate(origem, destino, userId, quando).getValue();
        historicos.salvar(historico);
        return historico;
    }

    private void criarUsuario(UUID empresaId, UUID usuarioId) {
        QuarkusTransaction.requiringNew().run(() -> {
            var empresa = new EmpresaEntity();
            empresa.id = empresaId;
            empresa.nome = "Funil";
            empresa.status = "ACTIVE";
            empresa.createdAt = Instant.parse("2026-01-01T00:00:00Z");
            empresa.updatedAt = empresa.createdAt;
            em.persist(empresa);
            var usuario = new UsuarioEmpresaEntity();
            usuario.id = usuarioId;
            usuario.empresaId = empresaId;
            usuario.nome = "Vendedor";
            usuario.email = "funil-" + usuarioId + "@funil.test";
            usuario.papel = "SELLER";
            usuario.status = "ACTIVE";
            usuario.createdAt = empresa.createdAt;
            usuario.updatedAt = empresa.createdAt;
            em.persist(usuario);
        });
    }

    private static void assertViolacaoUnica(Executable acao) {
        var falha = assertThrows(RuntimeException.class, acao);
        assertTrue(sqlState(falha, "23505"), descreve(falha));
    }

    private static boolean sqlState(Throwable falha, String estado) {
        for (Throwable t = falha; t != null; t = t.getCause()) {
            if (t instanceof SQLException sql && estado.equals(sql.getSQLState()))
                return true;
        }
        return false;
    }

    private static String descreve(Throwable falha) {
        var texto = new StringBuilder();
        for (Throwable t = falha; t != null; t = t.getCause())
            texto.append(t.getClass().getName()).append(": ").append(t.getMessage()).append(" | ");
        return texto.toString();
    }
}
