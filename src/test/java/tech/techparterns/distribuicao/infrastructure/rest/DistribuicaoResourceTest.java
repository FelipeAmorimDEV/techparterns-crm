package tech.techparterns.distribuicao.infrastructure.rest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.mailer.MockMailbox;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.jwt.Claim;
import io.quarkus.test.security.jwt.JwtSecurity;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import tech.techparterns.distribuicao.CapturaLeadAtribuido;
import tech.techparterns.distribuicao.application.TrocarDono;
import tech.techparterns.distribuicao.domain.model.DonoLead;
import tech.techparterns.distribuicao.domain.model.RegraAtribuicao;
import tech.techparterns.distribuicao.domain.model.ValidacaoDistribuicao;
import tech.techparterns.distribuicao.domain.port.FatosLead;
import tech.techparterns.distribuicao.domain.repository.DistribuicaoRepository;
import tech.techparterns.funil.application.usecase.MarcarGanhoUseCase;
import tech.techparterns.funil.application.usecase.MarcarPerdidoUseCase;
import tech.techparterns.funil.domain.model.TextoPlano;
import tech.techparterns.identidade.infrastructure.persistence.EmpresaEntity;
import tech.techparterns.identidade.infrastructure.persistence.UsuarioEmpresaEntity;
import tech.techparterns.lead.domain.model.*;
import tech.techparterns.lead.domain.repository.LeadRepository;
import tech.techparterns.shared.domain.CompanyId;

@QuarkusTest
class DistribuicaoResourceTest {

    private static final String CAB = "nome_contato,telefone,whatsapp,email\n";

    @Inject LeadRepository leads;
    @Inject DistribuicaoRepository distribuicao;
    @Inject FatosLead fatos;
    @Inject TrocarDono trocar;
    @Inject EntityManager em;
    @Inject CapturaLeadAtribuido captura;
    @Inject MockMailbox caixa;
    @Inject tech.techparterns.distribuicao.domain.port.VendedoresDistribuicao vendedores;

    @BeforeEach
    void limpar() {
        captura.limpar();
        caixa.clear();
    }

    @Test
    void outraEmpresaNaoVeConfiguracaoNemDono() {
        var a = "emp-iso-a";
        var b = "emp-iso-b";
        var lead = semearLead(a, "Ana", Instant.parse("2026-01-01T00:00:00Z"), SituacaoLead.ABERTA, null, null, false);
        distribuicao.salvarDono(new DonoLead(lead.getId(), a, null, null, false), Instant.now());
        distribuicao.salvarConfiguracao(new tech.techparterns.distribuicao.domain.model.ConfiguracaoEmpresa(
                a, true, RegraAtribuicao.RODIZIO, 5, false, 3));
        assertTrue(distribuicao.buscarDono(b, lead.getId()).isEmpty());
        assertTrue(distribuicao.buscarConfiguracao(b).isEmpty());
        assertEquals(3, distribuicao.buscarConfiguracao(a).orElseThrow().ponteiro());
    }

    @Test
    @TestSecurity(user = "u", roles = "admin")
    @JwtSecurity(claims = @Claim(key = "companyId", value = "ca02"))
    void importacaoComAutomaticaDesligadaNaoGeraDonoNemHistorico() {
        importar(CAB + "Ana,+55 (11) 98888-7777,,\n", "ignorar").statusCode(200);
        var lista = leads.listarNaoExcluidos(CompanyId.reconstituir("ca02"));
        assertEquals(1, lista.size());
        var lead = lista.get(0);
        assertEquals(SituacaoLead.ABERTA, lead.situacao());
        assertFalse(lead.quente());
        assertNull(lead.uf());
        assertNull(lead.cidade());
        assertTrue(distribuicao.buscarDono("ca02", lead.getId()).orElseThrow().ownerId() == null);
        assertTrue(distribuicao.historico("ca02", lead.getId()).isEmpty());
        assertTrue(captura.eventos().isEmpty());
        var fato = fatos.buscar("ca02", lead.getId()).orElseThrow();
        assertEquals("ABERTA", fato.situacao());
        assertNotNull(fato.criadoEm());
    }

    @Test
    @TestSecurity(user = "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaa1", roles = "ADMIN")
    @JwtSecurity(claims = @Claim(key = "companyId", value = "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaa01"))
    void automaticaRodizioAtribuiECompletarNaoMudaDono() {
        var empresa = UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaa01");
        var vendedor = UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaa2");
        empresa(empresa);
        usuario(vendedor, empresa, "SELLER", "ACTIVE", Instant.parse("2026-01-01T00:00:00Z"), "a2@ca03.test");
        configurar(true, "RODIZIO", 5, false);
        importar(CAB + "Ana,+55 (11) 98888-7001,,\n", "ignorar").statusCode(200);
        var lead = leads.listarNaoExcluidos(CompanyId.reconstituir(empresa.toString())).get(0);
        assertEquals(vendedor, distribuicao.buscarDono(empresa.toString(), lead.getId()).orElseThrow().ownerId());
        assertEquals(1, distribuicao.historico(empresa.toString(), lead.getId()).size());
        assertNull(distribuicao.historico(empresa.toString(), lead.getId()).get(0).autorId());
        assertEquals(1, captura.eventos().size());
        assertNull(captura.eventos().get(0).donoAnterior());
        assertEquals(vendedor, captura.eventos().get(0).donoNovo());
        importar(CAB + "Ana,+55 (11) 98888-7001,,ana@ca03.test\n", "completar_campos_vazios").statusCode(200);
        assertEquals(vendedor, distribuicao.buscarDono(empresa.toString(), lead.getId()).orElseThrow().ownerId());
        assertEquals(1, distribuicao.historico(empresa.toString(), lead.getId()).size());
    }

    @Test
    @TestSecurity(user = "u", roles = "ADMIN")
    @JwtSecurity(claims = @Claim(key = "companyId", value = "bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbb01"))
    void automaticaFilaNaoEscolheDono() {
        var empresa = UUID.fromString("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbb01");
        empresa(empresa);
        configurar(true, "FILA", 5, false);
        importar(CAB + "Ana,+55 (11) 98888-7002,,\n", "ignorar").statusCode(200);
        var lead = leads.listarNaoExcluidos(CompanyId.reconstituir(empresa.toString())).get(0);
        assertNull(distribuicao.buscarDono(empresa.toString(), lead.getId()).orElseThrow().ownerId());
        assertTrue(distribuicao.historico(empresa.toString(), lead.getId()).isEmpty());
    }

    @Test
    @TestSecurity(user = "cccccccc-cccc-4ccc-8ccc-ccccccccccc1", roles = "ADMIN")
    @JwtSecurity(claims = @Claim(key = "companyId", value = "cccccccc-cccc-4ccc-8ccc-cccccccccc01"))
    void manualTrocaDonoERejeitaDestinoInvalidoSemGravar() {
        var empresa = UUID.fromString("cccccccc-cccc-4ccc-8ccc-cccccccccc01");
        var admin = UUID.fromString("cccccccc-cccc-4ccc-8ccc-ccccccccccc1");
        var a = UUID.fromString("cccccccc-cccc-4ccc-8ccc-ccccccccccc2");
        var b = UUID.fromString("cccccccc-cccc-4ccc-8ccc-ccccccccccc3");
        var pendente = UUID.fromString("cccccccc-cccc-4ccc-8ccc-ccccccccccc4");
        empresa(empresa);
        usuario(admin, empresa, "ADMIN", "ACTIVE", Instant.parse("2026-01-01T00:00:00Z"), "admin@ca06.test");
        usuario(a, empresa, "SELLER", "ACTIVE", Instant.parse("2026-01-02T00:00:00Z"), "a@ca06.test");
        usuario(b, empresa, "SELLER", "ACTIVE", Instant.parse("2026-01-03T00:00:00Z"), "b@ca06.test");
        usuario(pendente, empresa, "SELLER", "PENDING", Instant.parse("2026-01-04T00:00:00Z"), "p@ca06.test");
        var aberto = semearLead(empresa.toString(), "Aberto", Instant.parse("2026-02-01T00:00:00Z"), SituacaoLead.ABERTA, null, null, false);
        var ganho = semearLead(empresa.toString(), "Ganho", Instant.parse("2026-02-02T00:00:00Z"), SituacaoLead.GANHO, null, null, false);
        given().contentType(ContentType.JSON)
                .body("{\"vendedorId\":\"" + pendente + "\",\"leadIds\":[\"" + aberto.getId() + "\"]}")
                .when().post("/distribuicao/atribuicoes").then().statusCode(400);
        assertTrue(distribuicao.historico(empresa.toString(), aberto.getId()).isEmpty());

        given().contentType(ContentType.JSON)
                .body("{\"vendedorId\":\"" + a + "\",\"leadIds\":[\"" + aberto.getId() + "\",\"" + ganho.getId() + "\"]}")
                .when().post("/distribuicao/atribuicoes").then().statusCode(200).body("atribuidos", is(1));
        assertEquals(a, distribuicao.buscarDono(empresa.toString(), aberto.getId()).orElseThrow().ownerId());
        assertTrue(distribuicao.buscarDono(empresa.toString(), ganho.getId()).isEmpty());

        given().contentType(ContentType.JSON)
                .body("{\"vendedorId\":\"" + b + "\",\"leadIds\":[\"" + aberto.getId() + "\"]}")
                .when().post("/distribuicao/atribuicoes").then().statusCode(200);
        fresco();
        var historico = distribuicao.historico(empresa.toString(), aberto.getId());
        assertEquals(2, historico.size());
        assertEquals(a, historico.get(0).donoNovo());
        assertEquals(a, historico.get(1).donoAnterior());
        assertEquals(b, historico.get(1).donoNovo());
        assertEquals(b, distribuicao.buscarDono(empresa.toString(), aberto.getId()).orElseThrow().ownerId());
        assertEquals(2, captura.eventos().size());
    }

    @Test
    @TestSecurity(user = "dddddddd-dddd-4ddd-8ddd-ddddddddddd1", roles = "ADMIN")
    @JwtSecurity(claims = @Claim(key = "companyId", value = "dddddddd-dddd-4ddd-8ddd-dddddddddd01"))
    void manualAceitaQuemEstaNoLimiteERegraNao() {
        var empresa = UUID.fromString("dddddddd-dddd-4ddd-8ddd-dddddddddd01");
        var admin = UUID.fromString("dddddddd-dddd-4ddd-8ddd-ddddddddddd1");
        var vendedor = UUID.fromString("dddddddd-dddd-4ddd-8ddd-ddddddddddd2");
        empresa(empresa);
        usuario(admin, empresa, "ADMIN", "ACTIVE", Instant.parse("2026-01-01T00:00:00Z"), "admin@ca07.test");
        usuario(vendedor, empresa, "SELLER", "ACTIVE", Instant.parse("2026-01-02T00:00:00Z"), "v@ca07.test");
        given().contentType(ContentType.JSON)
                .body("{\"peso\":1,\"maxLeadsAbertos\":0,\"participa\":false}")
                .when().put("/distribuicao/vendedores/" + vendedor + "/configuracao").then().statusCode(204);
        var lead = semearLead(empresa.toString(), "Fila", Instant.parse("2026-03-01T00:00:00Z"), SituacaoLead.ABERTA, null, null, false);
        distribuicao.salvarDono(new DonoLead(lead.getId(), empresa.toString(), null, null, false), Instant.now());
        given().contentType(ContentType.JSON).body("{\"regra\":\"RODIZIO\"}")
                .when().post("/distribuicao/lotes/confirmacao").then().statusCode(200).body("semDono", is(1));
        assertTrue(distribuicao.historico(empresa.toString(), lead.getId()).isEmpty());
        given().contentType(ContentType.JSON)
                .body("{\"vendedorId\":\"" + vendedor + "\",\"leadIds\":[\"" + lead.getId() + "\"]}")
                .when().post("/distribuicao/atribuicoes").then().statusCode(200).body("atribuidos", is(1));
        assertEquals(vendedor, distribuicao.buscarDono(empresa.toString(), lead.getId()).orElseThrow().ownerId());
    }

    @Test
    @TestSecurity(user = "abababab-abab-4aba-8aba-ababababab02", roles = "SELLER")
    @JwtSecurity(claims = @Claim(key = "companyId", value = "abababab-abab-4aba-8aba-ababababab10"))
    void vendedorNoLimiteNaoPegaFila() {
        var empresa = UUID.fromString("abababab-abab-4aba-8aba-ababababab10");
        var vendedor = UUID.fromString("abababab-abab-4aba-8aba-ababababab02");
        empresa(empresa);
        usuario(vendedor, empresa, "SELLER", "ACTIVE", Instant.parse("2026-01-02T00:00:00Z"), "limite@ca07b.test");
        QuarkusTransaction.requiringNew().run(() -> distribuicao.salvarConfigVendedor(
                new tech.techparterns.distribuicao.domain.model.ConfigVendedor(empresa.toString(), vendedor, 1, 0, true)));
        var lead = semearLead(empresa.toString(), "Parado", Instant.parse("2026-03-02T00:00:00Z"), SituacaoLead.ABERTA, null, null, false);
        distribuicao.salvarDono(new DonoLead(lead.getId(), empresa.toString(), null, null, false), Instant.now());
        given().when().post("/distribuicao/fila/proximo").then().statusCode(400);
        assertNull(distribuicao.buscarDono(empresa.toString(), lead.getId()).orElseThrow().ownerId());
    }

    @Test
    @TestSecurity(user = "admadmad-adma-4adm-8adm-admadmadad01", roles = "ADMIN")
    @JwtSecurity(claims = @Claim(key = "companyId", value = "admadmad-adma-4adm-8adm-admadmadad10"))
    void adminNaoPegaFilaNemDevolve() {
        given().when().post("/distribuicao/fila/proximo").then().statusCode(403);
        given().contentType(ContentType.JSON).body("{\"leadId\":\"" + UUID.randomUUID() + "\",\"motivo\":\"x\"}")
                .when().post("/distribuicao/fila/devolucoes").then().statusCode(403);
    }

    @Test
    @TestSecurity(user = "v", roles = "SELLER")
    @JwtSecurity(claims = @Claim(key = "companyId", value = "ca05-seller"))
    void sellerNaoListaSemDono() {
        given().when().get("/distribuicao/sem-dono").then().statusCode(403);
    }

    @Test
    @TestSecurity(user = "ffffffff-ffff-4fff-8fff-fffffffffff1", roles = "ADMIN")
    @JwtSecurity(claims = @Claim(key = "companyId", value = "ffffffff-ffff-4fff-8fff-ffffffffff01"))
    void listaSemDonoAplicaFiltrosEEscondeOResto() {
        var empresa = UUID.fromString("ffffffff-ffff-4fff-8fff-ffffffffff01");
        var admin = UUID.fromString("ffffffff-ffff-4fff-8fff-fffffffffff1");
        var dono = UUID.fromString("ffffffff-ffff-4fff-8fff-fffffffffff2");
        empresa(empresa);
        usuario(admin, empresa, "ADMIN", "ACTIVE", Instant.now(), "admin@ca05.test");
        usuario(dono, empresa, "SELLER", "ACTIVE", Instant.now(), "dono@ca05.test");
        var certo = semearLead(empresa.toString(), "Certo", Instant.parse("2026-04-01T00:00:00Z"), SituacaoLead.ABERTA, "SP", "São Paulo", true);
        semearLead(empresa.toString(), "OutraUf", Instant.parse("2026-04-02T00:00:00Z"), SituacaoLead.ABERTA, "RJ", "São Paulo", true);
        var ganho = semearLead(empresa.toString(), "Ganho", Instant.parse("2026-04-03T00:00:00Z"), SituacaoLead.GANHO, "SP", "São Paulo", true);
        var excluido = semearLead(empresa.toString(), "Excluido", Instant.parse("2026-04-04T00:00:00Z"), SituacaoLead.ABERTA, "SP", "São Paulo", true);
        excluido.excluirLogicamente(OffsetDateTime.now());
        leads.salvar(excluido);
        var comDono = semearLead(empresa.toString(), "ComDono", Instant.parse("2026-04-05T00:00:00Z"), SituacaoLead.ABERTA, "SP", "São Paulo", true);
        trocar.trocar(empresa.toString(), comDono.getId(), dono, RegraAtribuicao.MANUAL, null, admin);
        given().when().get("/distribuicao/sem-dono?uf=sp&cidade=São Paulo&quente=true&origem=IMPORTACAO")
                .then().statusCode(200).body("id", contains(certo.getId().toString()))
                .body("id", not(hasItem(ganho.getId().toString())));
    }

    @Test
    @TestSecurity(user = "12121212-1212-4121-8121-121212121201", roles = "ADMIN")
    @JwtSecurity(claims = @Claim(key = "companyId", value = "12121212-1212-4121-8121-121212121210"))
    void simulacaoNaoGravaEConfirmacaoReexecuta() {
        var empresa = UUID.fromString("12121212-1212-4121-8121-121212121210");
        var admin = UUID.fromString("12121212-1212-4121-8121-121212121201");
        var vendedor = UUID.fromString("12121212-1212-4121-8121-121212121202");
        empresa(empresa);
        usuario(admin, empresa, "ADMIN", "ACTIVE", Instant.parse("2026-01-01T00:00:00Z"), "admin@ca12.test");
        usuario(vendedor, empresa, "SELLER", "ACTIVE", Instant.parse("2026-01-02T00:00:00Z"), "v@ca12.test");
        var lead = semearLead(empresa.toString(), "L", Instant.parse("2026-05-01T00:00:00Z"), SituacaoLead.ABERTA, "SP", null, false);
        var outro = semearLead(empresa.toString(), "R", Instant.parse("2026-05-02T00:00:00Z"), SituacaoLead.ABERTA, "RJ", null, false);
        distribuicao.salvarDono(new DonoLead(lead.getId(), empresa.toString(), null, null, false), Instant.now());
        distribuicao.salvarDono(new DonoLead(outro.getId(), empresa.toString(), null, null, false), Instant.now());
        given().contentType(ContentType.JSON).body("{\"regra\":\"RODIZIO\"}")
                .when().post("/distribuicao/lotes/simulacao").then().statusCode(200)
                .body("porVendedor[0].quantidade", is(2)).body("semDono", is(0));
        assertTrue(distribuicao.historico(empresa.toString(), lead.getId()).isEmpty());
        assertTrue(captura.eventos().isEmpty());
        assertTrue(distribuicao.buscarConfiguracao(empresa.toString()).isEmpty());
        given().contentType(ContentType.JSON)
                .body("{\"peso\":1,\"maxLeadsAbertos\":0,\"participa\":true}")
                .when().put("/distribuicao/vendedores/" + vendedor + "/configuracao").then().statusCode(204);
        given().contentType(ContentType.JSON).body("{\"regra\":\"RODIZIO\",\"uf\":\"SP\"}")
                .when().post("/distribuicao/lotes/confirmacao").then().statusCode(200).body("semDono", is(1));
        assertNull(distribuicao.buscarDono(empresa.toString(), lead.getId()).orElseThrow().ownerId());
        assertTrue(distribuicao.historico(empresa.toString(), lead.getId()).isEmpty());
        assertNull(distribuicao.buscarDono(empresa.toString(), outro.getId()).orElseThrow().ownerId());
    }

    @Test
    @TestSecurity(user = "13131313-1313-4131-8131-131313131301", roles = "ADMIN")
    @JwtSecurity(claims = @Claim(key = "companyId", value = "13131313-1313-4131-8131-131313131310"))
    void loteParcialEFiltroDeUf() {
        var empresa = UUID.fromString("13131313-1313-4131-8131-131313131310");
        var admin = UUID.fromString("13131313-1313-4131-8131-131313131301");
        var vendedor = UUID.fromString("13131313-1313-4131-8131-131313131302");
        empresa(empresa);
        usuario(admin, empresa, "ADMIN", "ACTIVE", Instant.parse("2026-01-01T00:00:00Z"), "admin@ca13.test");
        usuario(vendedor, empresa, "SELLER", "ACTIVE", Instant.parse("2026-01-02T00:00:00Z"), "v@ca13.test");
        given().contentType(ContentType.JSON)
                .body("{\"peso\":1,\"maxLeadsAbertos\":1,\"participa\":true}")
                .when().put("/distribuicao/vendedores/" + vendedor + "/configuracao").then().statusCode(204);
        var primeiro = semearLead(empresa.toString(), "P", Instant.parse("2026-06-01T00:00:00Z"), SituacaoLead.ABERTA, "SP", null, false);
        var segundo = semearLead(empresa.toString(), "S", Instant.parse("2026-06-02T00:00:00Z"), SituacaoLead.ABERTA, "SP", null, false);
        var rj = semearLead(empresa.toString(), "J", Instant.parse("2026-06-03T00:00:00Z"), SituacaoLead.ABERTA, "RJ", null, false);
        for (var l : List.of(primeiro, segundo, rj))
            distribuicao.salvarDono(new DonoLead(l.getId(), empresa.toString(), null, null, false), Instant.now());
        given().contentType(ContentType.JSON).body("{\"regra\":\"RODIZIO\",\"uf\":\"SP\"}")
                .when().post("/distribuicao/lotes/confirmacao").then().statusCode(200)
                .body("semDono", is(1));
        assertEquals(vendedor, distribuicao.buscarDono(empresa.toString(), primeiro.getId()).orElseThrow().ownerId());
        assertNull(distribuicao.buscarDono(empresa.toString(), segundo.getId()).orElseThrow().ownerId());
        assertTrue(distribuicao.historico(empresa.toString(), segundo.getId()).isEmpty());
        assertNull(distribuicao.buscarDono(empresa.toString(), rj.getId()).orElseThrow().ownerId());
        assertTrue(distribuicao.historico(empresa.toString(), rj.getId()).isEmpty());
    }

    @Test
    @TestSecurity(user = "14141414-1414-4141-8141-141414141401", roles = "ADMIN")
    @JwtSecurity(claims = {
            @Claim(key = "companyId", value = "14141414-1414-4141-8141-141414141410"),
            @Claim(key = "sub", value = "14141414-1414-4141-8141-141414141401")
    })
    void desativarNaoMoveCarteiraEConfirmacaoDeixaGanho() {
        var empresa = UUID.fromString("14141414-1414-4141-8141-141414141410");
        var admin = UUID.fromString("14141414-1414-4141-8141-141414141401");
        var vendedor = UUID.fromString("14141414-1414-4141-8141-141414141402");
        empresa(empresa);
        usuario(admin, empresa, "ADMIN", "ACTIVE", Instant.parse("2026-01-01T00:00:00Z"), "admin@ca14.test");
        usuario(vendedor, empresa, "SELLER", "ACTIVE", Instant.parse("2026-01-02T00:00:00Z"), "v@ca14.test");
        var aberto = semearLead(empresa.toString(), "Aberto", Instant.parse("2026-07-01T00:00:00Z"), SituacaoLead.ABERTA, null, null, false);
        var ganho = semearLead(empresa.toString(), "Ganho", Instant.parse("2026-07-02T00:00:00Z"), SituacaoLead.GANHO, null, null, false);
        trocar.trocar(empresa.toString(), aberto.getId(), vendedor, RegraAtribuicao.MANUAL, null, admin);
        trocar.trocar(empresa.toString(), ganho.getId(), vendedor, RegraAtribuicao.MANUAL, null, admin);
        given().header("X-Forwarded-For", "10.14.0.1").when().post("/usuarios/" + vendedor + "/desativar").then().statusCode(204);
        assertEquals(vendedor, distribuicao.buscarDono(empresa.toString(), aberto.getId()).orElseThrow().ownerId());
        assertEquals(vendedor, distribuicao.buscarDono(empresa.toString(), ganho.getId()).orElseThrow().ownerId());
        given().contentType(ContentType.JSON).body("{\"regra\":\"RODIZIO\"}")
                .when().post("/distribuicao/carteiras/" + vendedor + "/confirmacao").then().statusCode(200);
        assertNull(distribuicao.buscarDono(empresa.toString(), aberto.getId()).orElseThrow().ownerId());
        assertEquals(vendedor, distribuicao.buscarDono(empresa.toString(), ganho.getId()).orElseThrow().ownerId());
    }

    @Test
    @TestSecurity(user = "15151515-1515-4151-8151-151515151502", roles = "SELLER")
    @JwtSecurity(claims = @Claim(key = "companyId", value = "15151515-1515-4151-8151-151515151510"))
    void filaRespeitaOrdemEDevolucao() {
        var empresa = UUID.fromString("15151515-1515-4151-8151-151515151510");
        var vendedor = UUID.fromString("15151515-1515-4151-8151-151515151502");
        empresa(empresa);
        usuario(vendedor, empresa, "SELLER", "ACTIVE", Instant.parse("2026-01-01T00:00:00Z"), "v@ca15.test");
        var t0 = Instant.parse("2026-08-01T00:00:00Z");
        var quenteVelho = semearLead(empresa.toString(), "QV", t0.plusSeconds(2), SituacaoLead.ABERTA, null, null, true);
        var quenteNovo = semearLead(empresa.toString(), "QN", t0.plusSeconds(5), SituacaoLead.ABERTA, null, null, false);
        var frio = semearLead(empresa.toString(), "FR", t0, SituacaoLead.ABERTA, null, null, false);
        var devolvido = semearLead(empresa.toString(), "DV", t0.plusSeconds(1), SituacaoLead.ABERTA, null, null, false);
        for (var l : List.of(quenteVelho, quenteNovo, frio, devolvido))
            distribuicao.salvarDono(new DonoLead(l.getId(), empresa.toString(), null, null, false), Instant.now());
        QuarkusTransaction.requiringNew().run(() -> em.createNativeQuery(
                        "update lead set quente = true where id = :id").setParameter("id", quenteNovo.getId()).executeUpdate());
        distribuicao.inserirAtribuicao(new tech.techparterns.distribuicao.domain.model.Atribuicao(
                UUID.randomUUID(), empresa.toString(), quenteVelho.getId(), null, null, RegraAtribuicao.DEVOLUCAO,
                "voltou", null, t0));
        distribuicao.inserirAtribuicao(new tech.techparterns.distribuicao.domain.model.Atribuicao(
                UUID.randomUUID(), empresa.toString(), devolvido.getId(), null, null, RegraAtribuicao.PARADO,
                null, null, t0));
        assertEquals(quenteVelho.getId(), proximo());
        assertEquals(quenteNovo.getId(), proximo());
        assertEquals(frio.getId(), proximo());
        assertEquals(devolvido.getId(), proximo());
        given().contentType(ContentType.JSON).body("{\"leadId\":\"" + devolvido.getId() + "\",\"motivo\":\"\"}")
                .when().post("/distribuicao/fila/devolucoes").then().statusCode(400);
        assertEquals(vendedor, distribuicao.buscarDono(empresa.toString(), devolvido.getId()).orElseThrow().ownerId());
        given().contentType(ContentType.JSON)
                .body("{\"leadId\":\"" + devolvido.getId() + "\",\"motivo\":\"" + "m".repeat(201) + "\"}")
                .when().post("/distribuicao/fila/devolucoes").then().statusCode(400);
        given().contentType(ContentType.JSON)
                .body("{\"leadId\":\"" + devolvido.getId() + "\",\"motivo\":\"cliente pediu\"}")
                .when().post("/distribuicao/fila/devolucoes").then().statusCode(204);
        fresco();
        assertNull(distribuicao.buscarDono(empresa.toString(), devolvido.getId()).orElseThrow().ownerId());
        assertEquals(RegraAtribuicao.DEVOLUCAO, distribuicao.historico(empresa.toString(), devolvido.getId())
                .get(distribuicao.historico(empresa.toString(), devolvido.getId()).size() - 1).regra());
        assertFalse(captura.eventos().isEmpty());
    }

    @Test
    void doisVendedoresNaoTravamaMesmoLead() throws Exception {
        var empresa = "conc-fila";
        var a = semearLead(empresa, "A", Instant.parse("2026-08-01T00:00:00Z"), SituacaoLead.ABERTA, null, null, false);
        var b = semearLead(empresa, "B", Instant.parse("2026-08-02T00:00:00Z"), SituacaoLead.ABERTA, null, null, true);
        distribuicao.salvarDono(new DonoLead(a.getId(), empresa, null, null, false), Instant.now());
        distribuicao.salvarDono(new DonoLead(b.getId(), empresa, null, null, false), Instant.now());
        var barreira = new CyclicBarrier(2);
        var ids = new CopyOnWriteArrayList<UUID>();
        var erros = new CopyOnWriteArrayList<Throwable>();
        Runnable tarefa = () -> {
            try {
                QuarkusTransaction.requiringNew().run(() -> {
                    ids.add(distribuicao.travarProximoDaFila(empresa).orElseThrow());
                    try {
                        barreira.await(5, TimeUnit.SECONDS);
                    } catch (Exception e) {
                        throw new IllegalStateException(e);
                    }
                });
            } catch (Exception e) {
                erros.add(e);
            }
        };
        var pool = Executors.newFixedThreadPool(2);
        var um = pool.submit(tarefa);
        var dois = pool.submit(tarefa);
        um.get(10, TimeUnit.SECONDS);
        dois.get(10, TimeUnit.SECONDS);
        pool.shutdown();
        assertTrue(erros.isEmpty(), () -> erros.toString());
        assertNotEquals(ids.get(0), ids.get(1));
    }

    @Test
    @TestSecurity(user = "17171717-1717-4171-8171-171717171701", roles = "ADMIN")
    @JwtSecurity(claims = {
            @Claim(key = "sub", value = "17171717-1717-4171-8171-171717171701"),
            @Claim(key = "companyId", value = "17171717-1717-4171-8171-171717171710")
    })
    void adminEditaFatosEFinalNaoVolta() {
        var empresa = UUID.fromString("17171717-1717-4171-8171-171717171710");
        var admin = UUID.fromString("17171717-1717-4171-8171-171717171701");
        empresa(empresa);
        usuario(admin, empresa, "ADMIN", "ACTIVE", Instant.now(), "admin@ca17.test");
        var lead = semearLead(empresa.toString(), "L", Instant.parse("2026-09-01T00:00:00Z"), SituacaoLead.ABERTA, null, null, false);
        given().contentType(ContentType.JSON)
                .body("{\"situacao\":\"ABERTA\",\"quente\":true,\"uf\":\"S\",\"cidade\":\"São Paulo\"}")
                .when().patch("/leads/" + lead.getId()).then().statusCode(400)
                .body("mensagem", is(ValidacaoDistribuicao.UF_INVALIDA));
        var aberto = leads.buscarPorId(lead.getId(), CompanyId.reconstituir(empresa.toString())).orElseThrow();
        assertEquals(SituacaoLead.ABERTA, aberto.situacao());
        assertFalse(aberto.quente());
        assertNull(aberto.uf());
        assertNull(aberto.cidade());
        given().contentType(ContentType.JSON)
                .body("{\"situacao\":\"GANHO\",\"quente\":true,\"uf\":\"sp\",\"cidade\":\"São Paulo\"}")
                .when().patch("/leads/" + lead.getId()).then().statusCode(400)
                .body("mensagem", is(TextoPlano.VAZIO + "; " + MarcarGanhoUseCase.VALOR_FECHADO_NAO_POSITIVO));
        fresco();
        var semPlano = leads.buscarPorId(lead.getId(), CompanyId.reconstituir(empresa.toString())).orElseThrow();
        assertEquals(SituacaoLead.ABERTA, semPlano.situacao());
        assertFalse(semPlano.quente());
        assertNull(semPlano.uf());
        assertNull(semPlano.cidade());
        var fechado = semearLead(empresa.toString(), "Fechado", Instant.parse("2026-09-01T01:00:00Z"),
                SituacaoLead.GANHO, "SP", "São Paulo", true);
        given().contentType(ContentType.JSON)
                .body("{\"situacao\":\"ABERTA\",\"quente\":false,\"uf\":null,\"cidade\":null}")
                .when().patch("/leads/" + fechado.getId()).then().statusCode(400)
                .body("mensagem", is("Situação final não muda"));
        fresco();
        var ainda = leads.buscarPorId(fechado.getId(), CompanyId.reconstituir(empresa.toString())).orElseThrow();
        assertEquals(SituacaoLead.GANHO, ainda.situacao());
        assertEquals("SP", ainda.uf());
        assertEquals("São Paulo", ainda.cidade());
        assertTrue(ainda.quente());
    }

    @Test
    @TestSecurity(user = "17171717-1717-4171-8171-171717171702", roles = "SELLER")
    @JwtSecurity(claims = {
            @Claim(key = "sub", value = "17171717-1717-4171-8171-171717171702"),
            @Claim(key = "companyId", value = "17171717-1717-4171-8171-171717171710")
    })
    void vendedorDonoNaoAlteraUf() {
        var empresa = "17171717-1717-4171-8171-171717171710";
        var dono = UUID.fromString("17171717-1717-4171-8171-171717171702");
        var outro = UUID.fromString("17171717-1717-4171-8171-171717171703");
        empresa(UUID.fromString(empresa));
        usuario(dono, UUID.fromString(empresa), "SELLER", "ACTIVE", Instant.now(), "dono@ca17b.test");
        usuario(outro, UUID.fromString(empresa), "SELLER", "ACTIVE", Instant.now(), "outro@ca17b.test");
        var meu = semearLead(empresa, "Meu", Instant.parse("2026-09-02T00:00:00Z"), SituacaoLead.ABERTA, "SP", "Campinas", false);
        var dele = semearLead(empresa, "Dele", Instant.parse("2026-09-03T00:00:00Z"), SituacaoLead.ABERTA, "RJ", null, false);
        trocar.trocar(empresa, meu.getId(), dono, RegraAtribuicao.MANUAL, null, null);
        trocar.trocar(empresa, dele.getId(), outro, RegraAtribuicao.MANUAL, null, null);
        given().contentType(ContentType.JSON)
                .body("{\"situacao\":\"ABERTA\",\"quente\":true,\"uf\":\"MG\",\"cidade\":\"BH\"}")
                .when().patch("/leads/" + meu.getId()).then().statusCode(204);
        fresco();
        var aberto = leads.buscarPorId(meu.getId(), CompanyId.reconstituir(empresa)).orElseThrow();
        assertEquals(SituacaoLead.ABERTA, aberto.situacao());
        assertEquals("SP", aberto.uf());
        assertEquals("Campinas", aberto.cidade());
        assertTrue(aberto.quente());
        given().contentType(ContentType.JSON)
                .body("{\"situacao\":\"PERDIDO\",\"quente\":false,\"uf\":\"RJ\",\"cidade\":\"BH\"}")
                .when().patch("/leads/" + meu.getId()).then().statusCode(400)
                .body("mensagem", is(MarcarPerdidoUseCase.MOTIVO_INVALIDO));
        fresco();
        var semMotivo = leads.buscarPorId(meu.getId(), CompanyId.reconstituir(empresa)).orElseThrow();
        assertEquals(SituacaoLead.ABERTA, semMotivo.situacao());
        assertEquals("SP", semMotivo.uf());
        assertEquals("Campinas", semMotivo.cidade());
        assertTrue(semMotivo.quente());
        given().contentType(ContentType.JSON)
                .body("{\"situacao\":\"GANHO\",\"quente\":true,\"uf\":\"MG\",\"cidade\":\"BH\"}")
                .when().patch("/leads/" + dele.getId()).then().statusCode(400)
                .body("mensagem", is(ValidacaoDistribuicao.NAO_E_DONO));
        fresco();
        var alheio = leads.buscarPorId(dele.getId(), CompanyId.reconstituir(empresa)).orElseThrow();
        assertEquals(SituacaoLead.ABERTA, alheio.situacao());
        assertEquals("RJ", alheio.uf());
        assertNull(alheio.cidade());
        assertFalse(alheio.quente());
    }

    @Test
    @TestSecurity(user = "18181818-1818-4181-8181-181818181801", roles = "ADMIN")
    @JwtSecurity(claims = @Claim(key = "companyId", value = "18181818-1818-4181-8181-181818181810"))
    void paradoMarcaSemDevolverEDevolveComEmail() {
        var empresa = UUID.fromString("18181818-1818-4181-8181-181818181810");
        var admin = UUID.fromString("18181818-1818-4181-8181-181818181801");
        var vendedor = UUID.fromString("18181818-1818-4181-8181-181818181802");
        empresa(empresa);
        usuario(admin, empresa, "ADMIN", "ACTIVE", Instant.now(), "admin@ca18.test");
        usuario(vendedor, empresa, "SELLER", "ACTIVE", Instant.now(), "vendedor@ca18.test");
        var exato = semearLead(empresa.toString(), "Exato", Instant.now(), SituacaoLead.ABERTA, null, null, false);
        var vencido = semearLead(empresa.toString(), "Vencido", Instant.now(), SituacaoLead.ABERTA, null, null, false);
        var ganho = semearLead(empresa.toString(), "Ganho", Instant.now(), SituacaoLead.GANHO, null, null, false);
        var agora = Instant.now();
        trocar.trocar(empresa.toString(), exato.getId(), vendedor, RegraAtribuicao.MANUAL, null, admin);
        trocar.trocar(empresa.toString(), vencido.getId(), vendedor, RegraAtribuicao.MANUAL, null, admin);
        trocar.trocar(empresa.toString(), ganho.getId(), vendedor, RegraAtribuicao.MANUAL, null, admin);
        marcarAtribuicao(exato.getId(), agora.minus(Duration.ofHours(120)).plus(Duration.ofMinutes(10)));
        marcarAtribuicao(vencido.getId(), agora.minus(Duration.ofHours(121)));
        marcarAtribuicao(ganho.getId(), agora.minus(Duration.ofHours(200)));
        given().contentType(ContentType.JSON)
                .body("{\"automatica\":false,\"regraPadrao\":null,\"diasParaParado\":5,\"devolverParadoAFila\":false}")
                .when().put("/distribuicao/configuracao").then().statusCode(204);
        given().when().post("/distribuicao/parados/verificacao").then().statusCode(200).body("marcados", is(1));
        assertFalse(distribuicao.buscarDono(empresa.toString(), exato.getId()).orElseThrow().parado());
        assertTrue(distribuicao.buscarDono(empresa.toString(), vencido.getId()).orElseThrow().parado());
        assertEquals(vendedor, distribuicao.buscarDono(empresa.toString(), vencido.getId()).orElseThrow().ownerId());
        assertFalse(distribuicao.buscarDono(empresa.toString(), ganho.getId()).orElseThrow().parado());
        assertTrue(caixa.getMailsSentTo("vendedor@ca18.test").isEmpty());
        given().contentType(ContentType.JSON)
                .body("{\"automatica\":false,\"diasParaParado\":5,\"devolverParadoAFila\":true}")
                .when().put("/distribuicao/configuracao").then().statusCode(204);
        captura.limpar();
        given().when().post("/distribuicao/parados/verificacao").then().statusCode(200).body("devolvidos", is(1));
        fresco();
        assertNull(distribuicao.buscarDono(empresa.toString(), vencido.getId()).orElseThrow().ownerId());
        assertEquals(RegraAtribuicao.PARADO, distribuicao.historico(empresa.toString(), vencido.getId())
                .get(distribuicao.historico(empresa.toString(), vencido.getId()).size() - 1).regra());
        assertEquals(1, captura.eventos().size());
        var emails = caixa.getMailsSentTo("vendedor@ca18.test");
        assertEquals(1, emails.size());
        assertTrue(emails.get(0).getText().contains("Vencido"));
        assertTrue(emails.get(0).getText().contains("voltou para a fila"));
    }

    @Test
    @TestSecurity(user = "u", roles = "admin")
    @JwtSecurity(claims = @Claim(key = "companyId", value = "20202020-2020-4202-8202-202020202010"))
    void exclusaoNaoApagaHistoricoNemDevolveAFila() {
        var empresa = "20202020-2020-4202-8202-202020202010";
        var vendedor = UUID.fromString("20202020-2020-4202-8202-202020202002");
        empresa(UUID.fromString(empresa));
        usuario(vendedor, UUID.fromString(empresa), "SELLER", "ACTIVE", Instant.now(), "v@ca20.test");
        var lead = semearLead(empresa, "X", Instant.parse("2026-10-01T00:00:00Z"), SituacaoLead.ABERTA, null, null, false);
        trocar.trocar(empresa, lead.getId(), vendedor, RegraAtribuicao.MANUAL, null, null);
        given().when().delete("/leads/" + lead.getId()).then().statusCode(204);
        assertEquals(1, distribuicao.historico(empresa, lead.getId()).size());
        assertTrue(fatos.listarSemDono(empresa, new FatosLead.Filtro(null, null, null, null)).stream()
                .noneMatch(f -> f.id().equals(lead.getId())));
        assertEquals(0, distribuicao.cargasAbertas(empresa).getOrDefault(vendedor, 0));
        distribuicao.salvarDono(new DonoLead(lead.getId(), empresa, null, null, false), Instant.now());
        assertTrue(distribuicao.travarProximoDaFila(empresa).isEmpty());
    }

    @Test
    @TestSecurity(user = "21212121-2121-4121-8121-212121212101", roles = "ADMIN")
    @JwtSecurity(claims = @Claim(key = "companyId", value = "21212121-2121-4121-8121-212121212110"))
    void adminDeOutraEmpresaNaoLeHistorico() {
        given().when().get("/distribuicao/leads/" + UUID.randomUUID() + "/historico").then().statusCode(404);
        given().when().get("/distribuicao/sem-dono").then().statusCode(200).body("", hasSize(0));
    }

    @Test
    @TestSecurity(user = "22222222-2222-4222-8222-222222222201", roles = "ADMIN")
    @JwtSecurity(claims = @Claim(key = "companyId", value = "22222222-2222-4222-8222-222222222210"))
    void configuracaoRejeitaInvalidoETerritorioDuplicado() {
        var empresa = UUID.fromString("22222222-2222-4222-8222-222222222210");
        var admin = UUID.fromString("22222222-2222-4222-8222-222222222201");
        var vendedor = UUID.fromString("22222222-2222-4222-8222-222222222202");
        var outraEmpresa = UUID.fromString("22222222-2222-4222-8222-222222222220");
        var outro = UUID.fromString("22222222-2222-4222-8222-222222222203");
        empresa(empresa);
        empresa(outraEmpresa);
        usuario(admin, empresa, "ADMIN", "ACTIVE", Instant.now(), "admin@ca22.test");
        usuario(vendedor, empresa, "SELLER", "ACTIVE", Instant.parse("2026-01-01T00:00:00Z"), "v@ca22.test");
        usuario(outro, outraEmpresa, "SELLER", "ACTIVE", Instant.now(), "outro@ca22.test");
        var cadastro = QuarkusTransaction.requiringNew().call(
                () -> vendedores.buscar(empresa.toString(), vendedor).orElseThrow());
        assertEquals("ACTIVE", cadastro.status());
        assertEquals(Instant.parse("2026-01-01T00:00:00Z"), cadastro.criadoEm());
        given().contentType(ContentType.JSON)
                .body("{\"peso\":11,\"maxLeadsAbertos\":null,\"participa\":true}")
                .when().put("/distribuicao/vendedores/" + vendedor + "/configuracao").then().statusCode(400);
        assertTrue(distribuicao.buscarConfigVendedor(empresa.toString(), vendedor).isEmpty());
        given().contentType(ContentType.JSON)
                .body("{\"automatica\":true,\"regraPadrao\":\"RODIZIO\",\"diasParaParado\":0,\"devolverParadoAFila\":false}")
                .when().put("/distribuicao/configuracao").then().statusCode(400);
        given().contentType(ContentType.JSON)
                .body("{\"uf\":\"sp\",\"cidade\":null,\"vendedorId\":\"" + outro + "\"}")
                .when().post("/distribuicao/territorios").then().statusCode(400);
        given().contentType(ContentType.JSON)
                .body("{\"uf\":\"sp\",\"cidade\":\"São Paulo\",\"vendedorId\":\"" + vendedor + "\"}")
                .when().post("/distribuicao/territorios").then().statusCode(201);
        given().contentType(ContentType.JSON)
                .body("{\"uf\":\"SP\",\"cidade\":\"São Paulo\",\"vendedorId\":\"" + vendedor + "\"}")
                .when().post("/distribuicao/territorios").then().statusCode(409).body("codigoErro", is("CONFLITO"));
        assertEquals(1, distribuicao.listarTerritorios(empresa.toString()).size());
        assertEquals("SP", distribuicao.listarTerritorios(empresa.toString()).get(0).uf());
    }

    @Test
    @TestSecurity(user = "seller-hist", roles = "SELLER")
    @JwtSecurity(claims = @Claim(key = "companyId", value = "hist-seller"))
    void sellerNaoLeHistorico() {
        given().when().get("/distribuicao/leads/" + UUID.randomUUID() + "/historico").then().statusCode(403);
    }

    private void fresco() {
        QuarkusTransaction.requiringNew().run(em::clear);
    }

    private UUID proximo() {
        return UUID.fromString(given().when().post("/distribuicao/fila/proximo").then().statusCode(200)
                .extract().path("leadId"));
    }

    private io.restassured.response.ValidatableResponse importar(String csv, String acao) {
        return given().multiPart("arquivo", "leads.csv", csv.getBytes(java.nio.charset.StandardCharsets.UTF_8), "text/csv")
                .multiPart("acao", acao).when().post("/leads/importacoes").then();
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

    private void marcarAtribuicao(UUID leadId, Instant quando) {
        QuarkusTransaction.requiringNew().run(() -> em.createNativeQuery(
                        "update lead_ownership set atribuido_em = :quando, parado = false where lead_id = :id")
                .setParameter("quando", quando).setParameter("id", leadId).executeUpdate());
    }

    private Lead semearLead(String empresa, String nome, Instant criado, SituacaoLead situacao, String uf, String cidade,
                            boolean quente) {
        var lead = Lead.reconstituir(UUID.randomUUID(), criado, criado, null, CompanyId.reconstituir(empresa),
                NomeContato.reconstituir(nome), TelefoneBrasil.reconstituir("+5511988887777"), null,
                EmailContato.reconstituir(UUID.randomUUID() + "@lead.test"), OrigemLead.IMPORTACAO,
                situacao, uf, cidade, quente);
        leads.salvar(lead);
        return lead;
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
}
