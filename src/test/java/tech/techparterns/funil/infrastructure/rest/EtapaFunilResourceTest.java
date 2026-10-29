package tech.techparterns.funil.infrastructure.rest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
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
import tech.techparterns.funil.application.usecase.AlterarEtapaUseCase;
import tech.techparterns.funil.application.usecase.ApagarEtapaUseCase;
import tech.techparterns.funil.application.usecase.DesativarEtapaUseCase;
import tech.techparterns.funil.domain.model.CorEtapa;
import tech.techparterns.funil.domain.model.Etapa;
import tech.techparterns.funil.domain.model.HistoricoEtapa;
import tech.techparterns.funil.domain.model.NegocioFunil;
import tech.techparterns.funil.domain.model.OrdemEtapa;
import tech.techparterns.funil.domain.model.Probabilidade;
import tech.techparterns.funil.domain.model.SlaDias;
import tech.techparterns.funil.domain.model.TipoEtapa;
import tech.techparterns.funil.domain.repository.EtapaRepository;
import tech.techparterns.funil.domain.repository.HistoricoEtapaRepository;
import tech.techparterns.funil.domain.repository.NegocioFunilRepository;
import tech.techparterns.funil.domain.service.NascimentoEtapas;
import tech.techparterns.lead.domain.model.Lead;
import tech.techparterns.lead.domain.model.NomeContato;
import tech.techparterns.lead.domain.model.TelefoneBrasil;
import tech.techparterns.lead.domain.repository.LeadRepository;
import tech.techparterns.shared.domain.CompanyId;

@QuarkusTest
class EtapaFunilResourceTest {

    private static final Instant TRANSICAO = Instant.parse("2026-04-01T15:30:00Z");

    @Inject
    EtapaRepository etapas;
    @Inject
    HistoricoEtapaRepository historicos;
    @Inject
    NegocioFunilRepository negocios;
    @Inject
    LeadRepository leads;
    @Inject
    EntityManager em;

    @Test
    @TestSecurity(user = "u", roles = "ADMIN")
    @JwtSecurity(claims = @Claim(key = "companyId", value = "funil-t7-aberto"))
    void adminPostAbertoGravaPedidoENasceFinais() {
        var empresa = CompanyId.reconstituir("funil-t7-aberto");

        criar(corpo("Proposta", "ABERTO", 8, true, 50, "#1A2B3C", 2, false, false));

        fresco();
        var lista = etapas.listarPorEmpresa(empresa);
        assertEquals(3, lista.size());
        assertEquals(1, lista.stream().filter(e -> e.tipo() == TipoEtapa.ABERTO).count());
        var pedida = porOrdem(lista, 8);
        assertEquals(TipoEtapa.ABERTO, pedida.tipo());
        assertTrue(pedida.ativa());
        assertEquals(50, pedida.probabilidade().valor());
        assertEquals("#1A2B3C", pedida.cor().valor());
        assertEquals(2, pedida.sla().valor());
        assertFalse(pedida.exigeQualificacao());
        assertFalse(pedida.exigeDemo());
        assertNascidaFinal(unica(lista, TipoEtapa.GANHO));
        assertNascidaFinal(unica(lista, TipoEtapa.PERDIDO));
    }

    @Test
    @TestSecurity(user = "u", roles = "ADMIN")
    @JwtSecurity(claims = @Claim(key = "companyId", value = "funil-t7-ganho"))
    void adminPostGanhoComoPrimeiraOperacao() {
        var empresa = CompanyId.reconstituir("funil-t7-ganho");

        criar(corpo("Fechamento", "GANHO", 5, true, null, null, null, false, false));

        fresco();
        var lista = etapas.listarPorEmpresa(empresa);
        assertEquals(3, lista.size());
        var pedida = porOrdem(lista, 5);
        assertEquals(TipoEtapa.GANHO, pedida.tipo());
        assertTrue(pedida.ativa());
        assertNull(pedida.probabilidade());
        assertNull(pedida.cor());
        assertTrue(pedida.sla().vazio());
        assertFalse(pedida.exigeQualificacao());
        assertFalse(pedida.exigeDemo());
        var aberto = unica(lista, TipoEtapa.ABERTO);
        assertNotNull(aberto.probabilidade());
        assertEquals(0, aberto.probabilidade().valor());
        assertTrue(aberto.sla().vazio());
        assertNascidaFinal(unica(lista, TipoEtapa.PERDIDO));
    }

    @Test
    @TestSecurity(user = "u", roles = "ADMIN")
    @JwtSecurity(claims = @Claim(key = "companyId", value = "funil-t7-perdido"))
    void adminPostPerdidoComoPrimeiraOperacao() {
        var empresa = CompanyId.reconstituir("funil-t7-perdido");

        criar(corpo("Encerrado", "PERDIDO", 9, true, null, null, null, false, false));

        fresco();
        var lista = etapas.listarPorEmpresa(empresa);
        assertEquals(3, lista.size());
        var pedida = porOrdem(lista, 9);
        assertEquals(TipoEtapa.PERDIDO, pedida.tipo());
        assertTrue(pedida.ativa());
        assertNull(pedida.probabilidade());
        assertNull(pedida.cor());
        assertTrue(pedida.sla().vazio());
        assertFalse(pedida.exigeQualificacao());
        assertFalse(pedida.exigeDemo());
        var aberto = unica(lista, TipoEtapa.ABERTO);
        assertNotNull(aberto.probabilidade());
        assertEquals(0, aberto.probabilidade().valor());
        assertTrue(aberto.sla().vazio());
        assertNascidaFinal(unica(lista, TipoEtapa.GANHO));
    }

    @Test
    @TestSecurity(user = "u", roles = "ADMIN")
    @JwtSecurity(claims = @Claim(key = "companyId", value = "funil-t7-invalido"))
    void corProbabilidadeESlaInvalidosNaoGravamEtapa() {
        var empresa = CompanyId.reconstituir("funil-t7-invalido");

        postRecusado(corpo("Contato", "ABERTO", 1, true, 50, "#FFF", 2, false, false), CorEtapa.INVALIDA);
        fresco();
        assertTrue(etapas.listarPorEmpresa(empresa).isEmpty());

        postRecusado(corpo("Contato", "ABERTO", 1, true, 101, "#1A2B3C", 2, false, false),
                Probabilidade.FORA_DA_FAIXA);
        fresco();
        assertTrue(etapas.listarPorEmpresa(empresa).isEmpty());

        postRecusado(corpo("Contato", "ABERTO", 1, true, 50, "#1A2B3C", 0, false, false), SlaDias.INVALIDO);
        fresco();
        assertTrue(etapas.listarPorEmpresa(empresa).isEmpty());

        postRecusado(corpo("Contato", "ABERTO", 1, true, 50, "#1A2B3C", -1, false, false), SlaDias.INVALIDO);
        fresco();
        assertTrue(etapas.listarPorEmpresa(empresa).isEmpty());
    }

    @Test
    @TestSecurity(user = "u", roles = "ADMIN")
    @JwtSecurity(claims = @Claim(key = "companyId", value = "funil-t7-prob0"))
    void probabilidadeZeroDeAbertoFicaGravada() {
        var empresa = CompanyId.reconstituir("funil-t7-prob0");

        criar(corpo("Contato", "ABERTO", 4, true, 0, null, null, false, false));

        fresco();
        var pedida = porOrdem(etapas.listarPorEmpresa(empresa), 4);
        assertEquals(TipoEtapa.ABERTO, pedida.tipo());
        assertNotNull(pedida.probabilidade());
        assertEquals(0, pedida.probabilidade().valor());
    }

    @Test
    @TestSecurity(user = "u", roles = "ADMIN")
    @JwtSecurity(claims = @Claim(key = "companyId", value = "funil-t7-ordem"))
    void ordemRepetidaNoPostPermanece() {
        var empresa = CompanyId.reconstituir("funil-t7-ordem");
        var id = criar(corpo("Existente", "ABERTO", 3, true, 10, null, null, false, false));

        fresco();
        var antes = etapas.listarPorEmpresa(empresa);
        assertEquals(3, antes.size());
        var gravada = antes.stream().filter(e -> e.getId().equals(id)).findFirst().orElseThrow();
        int ordemAntes = gravada.ordem().valor();
        assertEquals(3, ordemAntes);

        postRecusado(corpo("Nova", "ABERTO", ordemAntes, true, 20, null, null, false, false),
                NascimentoEtapas.ORDEM_REPETIDA);

        fresco();
        var depois = etapas.listarPorEmpresa(empresa);
        assertEquals(antes.size(), depois.size());
        var mesma = depois.stream().filter(e -> e.getId().equals(id)).findFirst().orElseThrow();
        assertEquals(ordemAntes, mesma.ordem().valor());
        for (Etapa anterior : antes) {
            var atual = depois.stream().filter(e -> e.getId().equals(anterior.getId())).findFirst().orElseThrow();
            assertEquals(anterior.ordem().valor(), atual.ordem().valor());
        }
    }

    @Test
    @TestSecurity(user = "u", roles = "ADMIN")
    @JwtSecurity(claims = @Claim(key = "companyId", value = "funil-t7-ganho-dup"))
    void segundaGanhoAtivaNaoNascePerdido() {
        var empresa = CompanyId.reconstituir("funil-t7-ganho-dup");
        var existente = Etapa.nascida(empresa, TipoEtapa.GANHO, OrdemEtapa.reconstituir(1));
        etapas.salvar(existente);

        postRecusado(corpo("Outro ganho", "GANHO", 4, true, null, null, null, false, false),
                NascimentoEtapas.GANHO_ATIVA_EXISTE);

        fresco();
        var lista = etapas.listarPorEmpresa(empresa);
        assertEquals(1, lista.size());
        assertEquals(existente.getId(), lista.getFirst().getId());
        assertEquals(1, lista.getFirst().ordem().valor());
        assertEquals(TipoEtapa.GANHO, lista.getFirst().tipo());
        assertTrue(lista.stream().noneMatch(e -> e.tipo() == TipoEtapa.PERDIDO));
    }

    @Test
    @TestSecurity(user = "u", roles = "ADMIN")
    @JwtSecurity(claims = @Claim(key = "companyId", value = "funil-t7-perdido-dup"))
    void segundaPerdidoAtivaNaoNasceGanho() {
        var empresa = CompanyId.reconstituir("funil-t7-perdido-dup");
        var existente = Etapa.nascida(empresa, TipoEtapa.PERDIDO, OrdemEtapa.reconstituir(1));
        etapas.salvar(existente);

        postRecusado(corpo("Outro perdido", "PERDIDO", 4, true, null, null, null, false, false),
                NascimentoEtapas.PERDIDO_ATIVA_EXISTE);

        fresco();
        var lista = etapas.listarPorEmpresa(empresa);
        assertEquals(1, lista.size());
        assertEquals(existente.getId(), lista.getFirst().getId());
        assertEquals(1, lista.getFirst().ordem().valor());
        assertEquals(TipoEtapa.PERDIDO, lista.getFirst().tipo());
        assertTrue(lista.stream().noneMatch(e -> e.tipo() == TipoEtapa.GANHO));
    }

    @Test
    @TestSecurity(user = "u", roles = "ADMIN")
    @JwtSecurity(claims = @Claim(key = "companyId", value = "funil-t7-patch"))
    void adminRenomeiaReordenaEGravaProbabilidadeCorESla() {
        var empresa = CompanyId.reconstituir("funil-t7-patch");
        var id = criar(corpo("Qualificacao", "ABERTO", 8, true, 50, "#1A2B3C", 2, true, true));

        fresco();
        var antes = etapas.buscarPorId(id, empresa).orElseThrow();
        assertEquals(TipoEtapa.ABERTO, antes.tipo());
        assertTrue(antes.ativa());
        assertTrue(antes.exigeQualificacao());
        assertTrue(antes.exigeDemo());

        given().contentType(ContentType.JSON)
                .body(corpoAlterar("Renomeada", 6, 40, "#ABCDEF", 5))
                .when().patch("/funil/etapas/" + id)
                .then().statusCode(204);

        fresco();
        var depois = etapas.buscarPorId(id, empresa).orElseThrow();
        assertEquals("Renomeada", depois.nome());
        assertEquals(6, depois.ordem().valor());
        assertEquals(40, depois.probabilidade().valor());
        assertEquals("#ABCDEF", depois.cor().valor());
        assertEquals(5, depois.sla().valor());
        assertTrue(depois.exigeQualificacao());
        assertTrue(depois.exigeDemo());
        assertEquals(TipoEtapa.ABERTO, depois.tipo());
        assertTrue(depois.ativa());
    }

    @Test
    @TestSecurity(user = "u", roles = "ADMIN")
    @JwtSecurity(claims = @Claim(key = "companyId", value = "funil-t7-patch-ordem"))
    void ordemRepetidaNoPatchPermanece() {
        var empresa = CompanyId.reconstituir("funil-t7-patch-ordem");
        criar(corpo("Primeira", "ABERTO", 4, true, 10, null, null, false, false));
        var segunda = criar(corpo("Segunda", "ABERTO", 7, true, 20, null, null, false, false));

        fresco();
        var antes = etapas.buscarPorId(segunda, empresa).orElseThrow();
        assertEquals("Segunda", antes.nome());
        assertEquals(7, antes.ordem().valor());

        given().contentType(ContentType.JSON)
                .body(corpoAlterar("Nao grava", 4, 30, null, null))
                .when().patch("/funil/etapas/" + segunda)
                .then().statusCode(400)
                .body("mensagem", is(NascimentoEtapas.ORDEM_REPETIDA));

        fresco();
        var depois = etapas.buscarPorId(segunda, empresa).orElseThrow();
        assertEquals("Segunda", depois.nome());
        assertEquals(7, depois.ordem().valor());
        assertEquals(4, porOrdem(etapas.listarPorEmpresa(empresa), 4).ordem().valor());
    }

    @Test
    @TestSecurity(user = "v", roles = "SELLER")
    @JwtSecurity(claims = @Claim(key = "companyId", value = "funil-t7-seller"))
    void vendedorNaoAlteraEtapa() {
        var empresa = CompanyId.reconstituir("funil-t7-seller");

        given().contentType(ContentType.JSON)
                .body(corpo("Nova", "ABERTO", 8, true, 50, "#1A2B3C", 2, false, false))
                .when().post("/funil/etapas")
                .then().statusCode(403);

        fresco();
        assertTrue(etapas.listarPorEmpresa(empresa).isEmpty());

        var etapa = Etapa.tryCreate(empresa.valor(), "Parada", TipoEtapa.ABERTO, 1, true,
                15, "#445566", 4, true, true).getValue();
        etapas.salvar(etapa);

        given().contentType(ContentType.JSON)
                .body(corpoAlterar("Mudou", 2, 90, "#FFFFFF", 9))
                .when().patch("/funil/etapas/" + etapa.getId())
                .then().statusCode(403);
        desativar(etapa.getId()).statusCode(403);
        given().when().delete("/funil/etapas/" + etapa.getId())
                .then().statusCode(403);

        fresco();
        assertEquals(1, etapas.listarPorEmpresa(empresa).size());
        var lida = etapas.buscarPorId(etapa.getId(), empresa).orElseThrow();
        assertEquals("Parada", lida.nome());
        assertEquals(TipoEtapa.ABERTO, lida.tipo());
        assertEquals(1, lida.ordem().valor());
        assertTrue(lida.ativa());
        assertEquals(15, lida.probabilidade().valor());
        assertEquals("#445566", lida.cor().valor());
        assertEquals(4, lida.sla().valor());
        assertTrue(lida.exigeQualificacao());
        assertTrue(lida.exigeDemo());
        assertNull(lida.getDeletedAt());
    }

    @Test
    @TestSecurity(user = "u", roles = "ADMIN")
    @JwtSecurity(claims = @Claim(key = "companyId", value = "funil-t7-com-lead"))
    void etapaComLeadNaoSomeEDesativaQuandoVazia() {
        var empresa = CompanyId.reconstituir("funil-t7-com-lead");
        var comLead = criar(corpo("Contato", "ABERTO", 4, true, 10, null, null, false, false));
        var outraAberta = criar(corpo("Visita", "ABERTO", 6, true, 20, null, null, false, false));
        var lead = gravarLead(empresa, "Helena", "+5511900007101");
        var usuario = UUID.randomUUID();
        var historico = gravarHistorico(comLead, usuario);
        gravarNegocio(lead.getId(), comLead);

        fresco();
        assertTrue(etapas.buscarPorId(outraAberta, empresa).orElseThrow().ativa());

        given().when().delete("/funil/etapas/" + comLead)
                .then().statusCode(400)
                .body("mensagem", is(ApagarEtapaUseCase.COM_LEAD));
        desativar(comLead).statusCode(400)
                .body("mensagem", is(DesativarEtapaUseCase.COM_LEAD));

        fresco();
        var ocupada = etapas.buscarPorId(comLead, empresa).orElseThrow();
        assertTrue(ocupada.ativa());
        assertNull(ocupada.getDeletedAt());
        assertHistorico(historico.getId(), empresa, comLead, usuario);

        apagarNegocio(lead.getId());

        desativar(comLead).statusCode(204);

        fresco();
        var desativada = etapas.buscarPorId(comLead, empresa).orElseThrow();
        assertFalse(desativada.ativa());
        assertHistorico(historico.getId(), empresa, comLead, usuario);
    }

    @Test
    @TestSecurity(user = "u", roles = "ADMIN")
    @JwtSecurity(claims = @Claim(key = "companyId", value = "funil-t7-ultimas"))
    void ultimaAbertaEUnicasFinaisNaoDesativam() {
        var empresa = CompanyId.reconstituir("funil-t7-ultimas");
        criar(corpo("Contato", "ABERTO", 4, true, 10, null, null, false, false));

        fresco();
        var lista = etapas.listarPorEmpresa(empresa);
        var aberta = unica(lista, TipoEtapa.ABERTO);
        var ganho = unica(lista, TipoEtapa.GANHO);
        var perdido = unica(lista, TipoEtapa.PERDIDO);

        desativar(aberta.getId()).statusCode(400)
                .body("mensagem", is(DesativarEtapaUseCase.ULTIMA_ABERTA));
        desativar(ganho.getId()).statusCode(400)
                .body("mensagem", is(DesativarEtapaUseCase.UNICA_GANHO));
        desativar(perdido.getId()).statusCode(400)
                .body("mensagem", is(DesativarEtapaUseCase.UNICA_PERDIDO));

        fresco();
        assertTrue(etapas.buscarPorId(aberta.getId(), empresa).orElseThrow().ativa());
        assertTrue(etapas.buscarPorId(ganho.getId(), empresa).orElseThrow().ativa());
        assertTrue(etapas.buscarPorId(perdido.getId(), empresa).orElseThrow().ativa());
    }

    @Test
    @TestSecurity(user = "u", roles = "ADMIN")
    @JwtSecurity(claims = @Claim(key = "companyId", value = "funil-t7-apaga"))
    void apagarSemLeadConservaHistoricoENaoTemCascade() {
        var empresa = CompanyId.reconstituir("funil-t7-apaga");
        var id = criar(corpo("Contato", "ABERTO", 4, true, 10, null, null, false, false));
        var usuario = UUID.randomUUID();
        var historico = gravarHistorico(id, usuario);

        given().when().delete("/funil/etapas/" + id)
                .then().statusCode(204);

        fresco();
        var apagada = etapas.buscarPorId(id, empresa).orElseThrow();
        assertNotNull(apagada.getDeletedAt());
        assertHistorico(historico.getId(), empresa, id, usuario);

        var tipos = confdeltypeDasFksDoHistorico();
        assertFalse(tipos.isEmpty());
        for (String tipo : tipos)
            assertNotEquals("c", tipo);
    }

    @Test
    @TestSecurity(user = "u", roles = "ADMIN")
    @JwtSecurity(claims = @Claim(key = "companyId", value = "funil-t7-alheia"))
    void etapaDeOutraEmpresaNaoMuda() {
        var outra = CompanyId.reconstituir("funil-t7-alheia-fora");
        var etapa = Etapa.tryCreate(outra.valor(), "Deles", TipoEtapa.ABERTO, 2, true,
                20, "#112233", 3, true, false).getValue();
        etapas.salvar(etapa);

        given().contentType(ContentType.JSON)
                .body(corpoAlterar("Invadiu", 8, 1, "#ABCDEF", 9))
                .when().patch("/funil/etapas/" + etapa.getId())
                .then().statusCode(400)
                .body("mensagem", is(AlterarEtapaUseCase.NAO_ENCONTRADA));
        given().when().delete("/funil/etapas/" + etapa.getId())
                .then().statusCode(400)
                .body("mensagem", is(ApagarEtapaUseCase.NAO_ENCONTRADA));

        fresco();
        var lida = etapas.buscarPorId(etapa.getId(), outra).orElseThrow();
        assertEquals("Deles", lida.nome());
        assertEquals(TipoEtapa.ABERTO, lida.tipo());
        assertEquals(2, lida.ordem().valor());
        assertTrue(lida.ativa());
        assertEquals(20, lida.probabilidade().valor());
        assertEquals("#112233", lida.cor().valor());
        assertEquals(3, lida.sla().valor());
        assertTrue(lida.exigeQualificacao());
        assertFalse(lida.exigeDemo());
        assertNull(lida.getDeletedAt());
    }

    private UUID criar(String corpo) {
        String id = given().contentType(ContentType.JSON)
                .body(corpo)
                .when().post("/funil/etapas")
                .then().statusCode(201)
                .extract().path("id");
        return UUID.fromString(id);
    }

    private static ValidatableResponse desativar(UUID id) {
        return given().contentType(ContentType.JSON)
                .when().post("/funil/etapas/" + id + "/desativar")
                .then();
    }

    private static void postRecusado(String corpo, String mensagem) {
        given().contentType(ContentType.JSON)
                .body(corpo)
                .when().post("/funil/etapas")
                .then().statusCode(400)
                .body("mensagem", is(mensagem));
    }

    private static String corpo(String nome, String tipo, int ordem, boolean ativa, Integer probabilidade,
                                String cor, Integer slaDias, boolean exigeQualificacao, boolean exigeDemo) {
        return "{"
                + "\"nome\":\"" + nome + "\","
                + "\"tipo\":\"" + tipo + "\","
                + "\"ordem\":" + ordem + ","
                + "\"ativa\":" + ativa + ","
                + "\"probabilidade\":" + json(probabilidade) + ","
                + "\"cor\":" + json(cor) + ","
                + "\"slaDias\":" + json(slaDias) + ","
                + "\"exigeQualificacao\":" + exigeQualificacao + ","
                + "\"exigeDemo\":" + exigeDemo
                + "}";
    }

    private static String corpoAlterar(String nome, int ordem, Integer probabilidade, String cor, Integer slaDias) {
        return "{"
                + "\"nome\":\"" + nome + "\","
                + "\"ordem\":" + ordem + ","
                + "\"probabilidade\":" + json(probabilidade) + ","
                + "\"cor\":" + json(cor) + ","
                + "\"slaDias\":" + json(slaDias)
                + "}";
    }

    private static String json(Integer valor) {
        return valor == null ? "null" : valor.toString();
    }

    private static String json(String valor) {
        return valor == null ? "null" : "\"" + valor + "\"";
    }

    private Lead gravarLead(CompanyId empresa, String nome, String telefone) {
        var criado = Lead.tryCreate(empresa, NomeContato.reconstituir(nome),
                TelefoneBrasil.reconstituir(telefone), null, null);
        assertTrue(criado.isOk());
        leads.salvar(criado.getValue());
        return criado.getValue();
    }

    private void gravarNegocio(UUID leadId, UUID etapaId) {
        var negocio = NegocioFunil.tryCreate(leadId, etapaId, TRANSICAO);
        assertTrue(negocio.isOk());
        negocios.salvar(negocio.getValue());
    }

    private HistoricoEtapa gravarHistorico(UUID destino, UUID userId) {
        var historico = HistoricoEtapa.tryCreate(null, destino, userId, TRANSICAO);
        assertTrue(historico.isOk());
        historicos.salvar(historico.getValue());
        return historico.getValue();
    }

    private void assertHistorico(UUID historicoId, CompanyId empresa, UUID destino, UUID userId) {
        var lido = historicos.buscarPorId(historicoId, empresa).orElseThrow();
        assertEquals(historicoId, lido.getId());
        assertNull(lido.origemEtapaId());
        assertEquals(destino, lido.destinoEtapaId());
        assertEquals(userId, lido.userId());
        assertEquals(TRANSICAO.toEpochMilli(), lido.quando().toEpochMilli());
    }

    private void apagarNegocio(UUID leadId) {
        QuarkusTransaction.requiringNew().run(() -> {
            var apagados = em.createNativeQuery("delete from funil_negocio where lead_id = :id")
                    .setParameter("id", leadId)
                    .executeUpdate();
            assertEquals(1, apagados);
            em.clear();
        });
    }

    private List<String> confdeltypeDasFksDoHistorico() {
        return QuarkusTransaction.requiringNew().call(() -> {
            @SuppressWarnings("unchecked")
            List<Object> linhas = em.createNativeQuery("""
                    select c.confdeltype
                    from pg_constraint c
                    join pg_class t on t.oid = c.conrelid
                    where t.relname = 'funil_historico_etapa'
                      and c.contype = 'f'
                    """).getResultList();
            return linhas.stream().map(EtapaFunilResourceTest::confdeltype).toList();
        });
    }

    private static String confdeltype(Object valor) {
        if (valor instanceof Character caractere)
            return caractere.toString();
        return valor.toString().trim();
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
        return encontradas.getFirst();
    }

    private static Etapa porOrdem(List<Etapa> lista, int ordem) {
        var encontradas = lista.stream().filter(e -> e.ordem().valor() == ordem).toList();
        assertEquals(1, encontradas.size());
        return encontradas.getFirst();
    }

    private void fresco() {
        QuarkusTransaction.requiringNew().run(em::clear);
    }
}
