package tech.techparterns.funil.application;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.jwt.Claim;
import io.quarkus.test.security.jwt.JwtSecurity;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import tech.techparterns.funil.domain.model.Etapa;
import tech.techparterns.funil.domain.model.LeadGanho;
import tech.techparterns.funil.domain.model.LeadMudouEtapa;
import tech.techparterns.funil.domain.model.LeadPerdido;
import tech.techparterns.funil.domain.model.NegocioFunil;
import tech.techparterns.funil.domain.model.TipoEtapa;
import tech.techparterns.funil.domain.model.ValorComercial;
import tech.techparterns.funil.domain.port.ConsultaLeadFunil;
import tech.techparterns.funil.domain.repository.EtapaRepository;
import tech.techparterns.funil.domain.repository.NegocioFunilRepository;
import tech.techparterns.funil.domain.service.ValorColuna;
import tech.techparterns.funil.infrastructure.persistence.HistoricoEtapaEntity;
import tech.techparterns.identidade.infrastructure.persistence.EmpresaEntity;
import tech.techparterns.identidade.infrastructure.persistence.UsuarioEmpresaEntity;
import tech.techparterns.lead.domain.model.Lead;
import tech.techparterns.lead.domain.model.SituacaoLead;
import tech.techparterns.lead.domain.repository.LeadRepository;
import tech.techparterns.shared.domain.CompanyId;

@QuarkusTest
class EntradaLeadCriadoTest {

    private static final String CAB = "nome_contato,telefone,whatsapp,email\n";

    @Inject
    LeadRepository leads;

    @Inject
    EtapaRepository etapas;

    @Inject
    NegocioFunilRepository negocios;

    @Inject
    ConsultaLeadFunil consulta;

    @Inject
    CapturaEventosFunil captura;

    @Inject
    EntityManager em;

    @Test
    @TestSecurity(user = "admin-funil-t4-sem-dono", roles = "ADMIN")
    @JwtSecurity(claims = @Claim(key = "companyId", value = "funil-t4-sem-dono"))
    void importacaoSemDonoEntraNaAbertoAtivaDeMenorOrdem() {
        var empresa = CompanyId.reconstituir("funil-t4-sem-dono");
        configurar(false, "RODIZIO", 5, false);

        importar(CAB + "Ana,+55 (11) 98888-7701,,\n", "ignorar")
                .statusCode(200)
                .body("criados", is(1));

        var lead = unicoLead(empresa);
        var aberto = abertoAtivaDeMenorOrdem(etapas.listarPorEmpresa(empresa));
        var negocio = negocios.buscar(lead.getId(), empresa).orElseThrow();
        assertEquals(aberto.getId(), negocio.etapaId());
        assertNull(consulta.buscar(empresa, lead.getId()).orElseThrow().dono());
        assertHistoricoDeEntrada(negocio.etapaId());
        assertSoLeadMudouEtapa();
    }

    @Test
    @TestSecurity(user = "4f4f4f01-4f4f-44f4-84f4-4f4f4f4f4f03", roles = "ADMIN")
    @JwtSecurity(claims = @Claim(key = "companyId", value = "4f4f4f01-4f4f-44f4-84f4-4f4f4f4f4f01"))
    void importacaoComDonoDeRodizioEntraNaMesmaAberto() {
        var empresaId = UUID.fromString("4f4f4f01-4f4f-44f4-84f4-4f4f4f4f4f01");
        var vendedor = UUID.fromString("4f4f4f01-4f4f-44f4-84f4-4f4f4f4f4f02");
        var empresa = CompanyId.reconstituir(empresaId.toString());
        empresa(empresaId);
        usuario(vendedor, empresaId, "SELLER", "ACTIVE", Instant.parse("2026-01-01T00:00:00Z"),
                "vendedor@funil-t4-rodizio.test");
        configurar(true, "RODIZIO", 5, false);

        importar(CAB + "Ana,+55 (11) 98888-7702,,\n", "ignorar")
                .statusCode(200)
                .body("criados", is(1));

        var lead = unicoLead(empresa);
        var aberto = abertoAtivaDeMenorOrdem(etapas.listarPorEmpresa(empresa));
        var negocio = negocios.buscar(lead.getId(), empresa).orElseThrow();
        assertEquals(aberto.getId(), negocio.etapaId());
        assertEquals(vendedor, consulta.buscar(empresa, lead.getId()).orElseThrow().dono());
        assertHistoricoDeEntrada(negocio.etapaId());
        assertSoLeadMudouEtapa();
    }

    @Test
    @TestSecurity(user = "admin-funil-t4-ordens", roles = "ADMIN")
    @JwtSecurity(claims = @Claim(key = "companyId", value = "funil-t4-ordens"))
    void duasAbertoAtivasLeadEntraNaDeOrdemUm() {
        var empresa = "funil-t4-ordens";
        var company = CompanyId.reconstituir(empresa);
        gravarEtapa(empresa, "Coluna um", TipoEtapa.ABERTO, 1);
        gravarEtapa(empresa, "Coluna dois", TipoEtapa.ABERTO, 2);
        gravarEtapa(empresa, "Final ganho", TipoEtapa.GANHO, 3);
        gravarEtapa(empresa, "Final perdido", TipoEtapa.PERDIDO, 4);

        importar(CAB + "Ana,+55 (11) 98888-7703,,\n", "ignorar")
                .statusCode(200)
                .body("criados", is(1));

        var lista = etapas.listarPorEmpresa(company);
        var ordem1 = porOrdem(lista, 1);
        var ordem2 = porOrdem(lista, 2);
        assertEquals(TipoEtapa.ABERTO, ordem1.tipo());
        assertEquals(TipoEtapa.ABERTO, ordem2.tipo());
        assertTrue(ordem1.ativa());
        assertTrue(ordem2.ativa());
        var negocio = negocios.buscar(unicoLead(company).getId(), company).orElseThrow();
        assertEquals(ordem1.getId(), negocio.etapaId());
        assertNotEquals(ordem2.getId(), negocio.etapaId());
        assertHistoricoDeEntrada(ordem1.getId());
    }

    @Test
    @TestSecurity(user = "admin-funil-t4-nascidas", roles = "ADMIN")
    @JwtSecurity(claims = @Claim(key = "companyId", value = "funil-t4-nascidas"))
    void empresaSemEtapasNasceTresEValorPrevistoZero() {
        var empresa = CompanyId.reconstituir("funil-t4-nascidas");

        importar(CAB + "Ana,+55 (11) 98888-7704,,\n", "ignorar")
                .statusCode(200)
                .body("criados", is(1));

        var lista = etapas.listarPorEmpresa(empresa);
        assertEquals(3, lista.size());
        var aberto = unica(lista, TipoEtapa.ABERTO);
        assertNascidaAberta(aberto);
        assertNascidaFinal(unica(lista, TipoEtapa.GANHO));
        assertNascidaFinal(unica(lista, TipoEtapa.PERDIDO));
        var lead = unicoLead(empresa);
        var negocio = negocios.buscar(lead.getId(), empresa).orElseThrow();
        assertEquals(aberto.getId(), negocio.etapaId());

        var cem = ValorComercial.tryCreate(new BigDecimal("100.00"));
        assertTrue(cem.isOk());
        assertEquals(2, cem.getValue().valor().scale());
        negocios.salvar(NegocioFunil.reconstituir(
                negocio.getId(), negocio.getCreatedAt(), Instant.now(), negocio.getDeletedAt(),
                negocio.leadId(), negocio.etapaId(), negocio.entrouNaEtapa(),
                negocio.decisor(), negocio.dorPrincipal(), negocio.usuariosPrevistos(),
                negocio.prazoDecisao(), negocio.orcamento(), negocio.dataDemo(),
                cem.getValue(), negocio.planoInteresse(), negocio.previsaoFechamento(),
                negocio.planoFechado(), negocio.valorFechado(), negocio.motivoPerdaId(),
                negocio.observacao()));

        var relido = negocios.buscar(lead.getId(), empresa).orElseThrow();
        assertEquals(new BigDecimal("100.00"), relido.valorEstimado().valor());
        assertEquals(2, relido.valorEstimado().valor().scale());
        var previsto = ValorColuna.contribuicao(
                TipoEtapa.ABERTO, relido.valorEstimado(), relido.valorFechado(), aberto.probabilidade());
        assertEquals(new BigDecimal("0.00"), previsto.valor());
        assertEquals(2, previsto.valor().scale());
    }

    @Test
    @TestSecurity(user = "admin-funil-t4-ignorar", roles = "ADMIN")
    @JwtSecurity(claims = @Claim(key = "companyId", value = "funil-t4-ignorar"))
    void ignorarNaoCriaNegocioNemAlteraEtapa() {
        var empresa = CompanyId.reconstituir("funil-t4-ignorar");
        importar(CAB + "Ana,+55 (11) 98888-7705,,\n", "ignorar")
                .statusCode(200)
                .body("criados", is(1));
        var lead = unicoLead(empresa);
        var etapaAntes = negocios.buscar(lead.getId(), empresa).orElseThrow().etapaId();
        var negocioAntes = negocios.buscar(lead.getId(), empresa).orElseThrow().getId();

        importar(CAB + "Ana,+55 (11) 98888-7705,,\n", "ignorar")
                .statusCode(200)
                .body("criados", is(0))
                .body("ignorados", is(1));

        assertEquals(1, leads.listarNaoExcluidos(empresa).size());
        assertEquals(1, contarNegocios(empresa.valor()));
        var negocio = negocios.buscar(lead.getId(), empresa).orElseThrow();
        assertEquals(negocioAntes, negocio.getId());
        assertEquals(etapaAntes, negocio.etapaId());
        assertEquals(0, captura.ocorrencias(LeadMudouEtapa.class));
        assertEquals(0, captura.ocorrencias(LeadGanho.class));
        assertEquals(0, captura.ocorrencias(LeadPerdido.class));
    }

    @Test
    @TestSecurity(user = "admin-funil-t4-ganho", roles = "ADMIN")
    @JwtSecurity(claims = @Claim(key = "companyId", value = "funil-t4-ganho"))
    void completarCamposVaziosNaoAlteraEtapaDeGanho() {
        completarNaoAlteraFinal("funil-t4-ganho", "+55 (11) 98888-7706", "ganho@funil-t4.test",
                TipoEtapa.GANHO, SituacaoLead.GANHO);
    }

    @Test
    @TestSecurity(user = "admin-funil-t4-perdido", roles = "ADMIN")
    @JwtSecurity(claims = @Claim(key = "companyId", value = "funil-t4-perdido"))
    void completarCamposVaziosNaoAlteraEtapaDePerdido() {
        completarNaoAlteraFinal("funil-t4-perdido", "+55 (11) 98888-7707", "perdido@funil-t4.test",
                TipoEtapa.PERDIDO, SituacaoLead.PERDIDO);
    }

    @Test
    @TestSecurity(user = "admin-funil-t4-mesmo", roles = "ADMIN")
    @JwtSecurity(claims = @Claim(key = "companyId", value = "funil-t4-mesmo"))
    void criarMesmoAssimCriaOutroNaPrimeiraAbertoEConservaEtapaAntiga() {
        var empresa = "funil-t4-mesmo";
        var company = CompanyId.reconstituir(empresa);
        gravarEtapa(empresa, "Coluna um", TipoEtapa.ABERTO, 1);
        gravarEtapa(empresa, "Coluna dois", TipoEtapa.ABERTO, 2);
        gravarEtapa(empresa, "Final ganho", TipoEtapa.GANHO, 3);
        gravarEtapa(empresa, "Final perdido", TipoEtapa.PERDIDO, 4);
        importar(CAB + "Ana,+55 (11) 98888-7708,,\n", "ignorar")
                .statusCode(200)
                .body("criados", is(1));

        var lista = etapas.listarPorEmpresa(company);
        var ordem1 = porOrdem(lista, 1);
        var ordem2 = porOrdem(lista, 2);
        var antigo = unicoLead(company);
        reposicionarEtapa(company, antigo, ordem2.getId());

        importar(CAB + "Bia,+55 (11) 98888-7708,,\n", "criar_mesmo_assim")
                .statusCode(200)
                .body("criadosMesmoAssim", is(1));

        var todos = leads.listarNaoExcluidos(company);
        assertEquals(2, todos.size());
        var novo = todos.stream().filter(lead -> !lead.getId().equals(antigo.getId())).findFirst().orElseThrow();
        assertEquals(ordem2.getId(), negocios.buscar(antigo.getId(), company).orElseThrow().etapaId());
        assertEquals(ordem1.getId(), negocios.buscar(novo.getId(), company).orElseThrow().etapaId());
        assertEquals(TipoEtapa.ABERTO, ordem1.tipo());
        assertTrue(ordem1.ativa());
        assertSoLeadMudouEtapa();
    }

    private void completarNaoAlteraFinal(String empresaId, String telefone, String email,
                                         TipoEtapa tipoFinal, SituacaoLead situacao) {
        var empresa = CompanyId.reconstituir(empresaId);
        importar(CAB + "Ana," + telefone + ",,\n", "ignorar")
                .statusCode(200)
                .body("criados", is(1));
        var lead = unicoLead(empresa);
        var finalAtiva = unica(etapas.listarPorEmpresa(empresa), tipoFinal);
        assertTrue(finalAtiva.ativa());
        reposicionarEtapa(empresa, lead, finalAtiva.getId());
        var comSituacao = leads.buscarPorId(lead.getId(), empresa).orElseThrow()
                .comFatos(situacao, false, null, null, Instant.now());
        assertTrue(comSituacao.isOk());
        leads.salvar(comSituacao.getValue());
        assertEquals(0, captura.ocorrencias(LeadGanho.class));
        assertEquals(0, captura.ocorrencias(LeadPerdido.class));

        importar(CAB + "Ana," + telefone + ",," + email + "\n", "completar_campos_vazios")
                .statusCode(200)
                .body("completados", is(1))
                .body("criados", is(0));

        var relido = leads.buscarPorId(lead.getId(), empresa).orElseThrow();
        assertEquals(situacao, relido.situacao());
        assertEquals(email, relido.email().valor());
        assertEquals(finalAtiva.getId(), negocios.buscar(lead.getId(), empresa).orElseThrow().etapaId());
        assertEquals(1, contarNegocios(empresaId));
        assertEquals(0, captura.ocorrencias(LeadMudouEtapa.class));
        assertEquals(0, captura.ocorrencias(LeadGanho.class));
        assertEquals(0, captura.ocorrencias(LeadPerdido.class));
    }

    private void reposicionarEtapa(CompanyId empresa, Lead lead, UUID etapaId) {
        var negocio = negocios.buscar(lead.getId(), empresa).orElseThrow();
        negocios.salvar(NegocioFunil.reconstituir(
                negocio.getId(), negocio.getCreatedAt(), Instant.now(), negocio.getDeletedAt(),
                negocio.leadId(), etapaId, negocio.entrouNaEtapa(),
                negocio.decisor(), negocio.dorPrincipal(), negocio.usuariosPrevistos(),
                negocio.prazoDecisao(), negocio.orcamento(), negocio.dataDemo(),
                negocio.valorEstimado(), negocio.planoInteresse(), negocio.previsaoFechamento(),
                negocio.planoFechado(), negocio.valorFechado(), negocio.motivoPerdaId(),
                negocio.observacao()));
    }

    private void gravarEtapa(String empresa, String nome, TipoEtapa tipo, int ordem) {
        var criada = Etapa.tryCreate(empresa, nome, tipo, ordem, true, null, null, null, false, false);
        assertTrue(criada.isOk());
        etapas.salvar(criada.getValue());
    }

    private ValidatableResponse importar(String csv, String acao) {
        captura.limpar();
        return given().multiPart("arquivo", "leads.csv", csv.getBytes(StandardCharsets.UTF_8), "text/csv")
                .multiPart("acao", acao)
                .when().post("/leads/importacoes").then();
    }

    private void configurar(boolean automatica, String regra, int dias, boolean devolver) {
        given().contentType(ContentType.JSON)
                .body(java.util.Map.of(
                        "automatica", automatica,
                        "regraPadrao", regra,
                        "diasParaParado", dias,
                        "devolverParadoAFila", devolver))
                .when().put("/distribuicao/configuracao").then().statusCode(204);
    }

    private void empresa(UUID id) {
        QuarkusTransaction.requiringNew().run(() -> {
            if (em.find(EmpresaEntity.class, id) != null)
                return;
            var e = new EmpresaEntity();
            e.id = id;
            e.nome = "Empresa";
            e.status = "ACTIVE";
            e.createdAt = Instant.parse("2026-01-01T00:00:00Z");
            e.updatedAt = e.createdAt;
            em.persist(e);
        });
    }

    private void usuario(UUID id, UUID empresa, String papel, String status, Instant criado, String email) {
        QuarkusTransaction.requiringNew().run(() -> {
            if (em.find(UsuarioEmpresaEntity.class, id) != null)
                return;
            var u = new UsuarioEmpresaEntity();
            u.id = id;
            u.empresaId = empresa;
            u.nome = "Pessoa";
            u.email = email;
            u.papel = papel;
            u.status = status;
            u.createdAt = criado;
            u.updatedAt = criado;
            em.persist(u);
        });
    }

    private Lead unicoLead(CompanyId empresa) {
        var lista = leads.listarNaoExcluidos(empresa);
        assertEquals(1, lista.size());
        return lista.get(0);
    }

    private long contarNegocios(String empresa) {
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

    private void assertHistoricoDeEntrada(UUID destino) {
        var linhas = QuarkusTransaction.requiringNew().call(() -> {
            em.clear();
            return em.createQuery(
                            "from HistoricoEtapaEntity h where h.destinoEtapaId = :destino",
                            HistoricoEtapaEntity.class)
                    .setParameter("destino", destino)
                    .getResultList();
        });
        assertEquals(1, linhas.size());
        assertNull(linhas.get(0).origemEtapaId);
        assertEquals(destino, linhas.get(0).destinoEtapaId);
        assertNull(linhas.get(0).userId);
    }

    private void assertSoLeadMudouEtapa() {
        assertEquals(1, captura.ocorrencias(LeadMudouEtapa.class));
        assertEquals(0, captura.ocorrencias(LeadGanho.class));
        assertEquals(0, captura.ocorrencias(LeadPerdido.class));
        assertEquals(1, captura.eventos().size());
    }

    private static void assertNascidaAberta(Etapa etapa) {
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
