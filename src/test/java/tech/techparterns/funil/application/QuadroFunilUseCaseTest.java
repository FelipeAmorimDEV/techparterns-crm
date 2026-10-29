package tech.techparterns.funil.application;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.jwt.Claim;
import io.quarkus.test.security.jwt.JwtSecurity;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.ws.rs.QueryParam;
import tech.techparterns.distribuicao.domain.model.DonoLead;
import tech.techparterns.distribuicao.domain.repository.DistribuicaoRepository;
import tech.techparterns.funil.application.command.ColocarLeadsJaGravadosCommand;
import tech.techparterns.funil.application.dto.QuadroFunilDto;
import tech.techparterns.funil.application.query.QuadroFunilQuery;
import tech.techparterns.funil.application.usecase.ColocarLeadsJaGravadosUseCase;
import tech.techparterns.funil.application.usecase.GarantirEtapasUseCase;
import tech.techparterns.funil.application.usecase.QuadroFunilUseCase;
import tech.techparterns.funil.domain.model.Etapa;
import tech.techparterns.funil.domain.model.MotivoPerda;
import tech.techparterns.funil.domain.model.NegocioFunil;
import tech.techparterns.funil.domain.model.OrdemEtapa;
import tech.techparterns.funil.domain.model.TipoEtapa;
import tech.techparterns.funil.domain.model.ValorComercial;
import tech.techparterns.funil.domain.repository.EtapaRepository;
import tech.techparterns.funil.domain.repository.MotivoPerdaRepository;
import tech.techparterns.funil.domain.repository.NegocioFunilRepository;
import tech.techparterns.funil.infrastructure.rest.QuadroFunilResource;
import tech.techparterns.identidade.infrastructure.persistence.EmpresaEntity;
import tech.techparterns.identidade.infrastructure.persistence.UsuarioEmpresaEntity;
import tech.techparterns.lead.domain.model.EmailContato;
import tech.techparterns.lead.domain.model.Lead;
import tech.techparterns.lead.domain.model.NomeContato;
import tech.techparterns.lead.domain.model.OrigemLead;
import tech.techparterns.lead.domain.model.SituacaoLead;
import tech.techparterns.lead.domain.model.TelefoneBrasil;
import tech.techparterns.lead.domain.repository.LeadRepository;
import tech.techparterns.shared.domain.CompanyId;

@QuarkusTest
class QuadroFunilUseCaseTest {

    private static final Instant ENTRADA = Instant.parse("2026-04-01T15:30:00Z");
    private static final List<String> CAMPOS_AUSENTES_NO_ITEM = List.of(
            "nome", "telefone", "email", "e-mail", "plano", "roteiro", "cultura", "tag", "follow-up", "followUp");

    @Inject
    EtapaRepository etapas;
    @Inject
    MotivoPerdaRepository motivos;
    @Inject
    NegocioFunilRepository negocios;
    @Inject
    LeadRepository leads;
    @Inject
    DistribuicaoRepository distribuicao;
    @Inject
    ColocarLeadsJaGravadosUseCase colocar;
    @Inject
    GarantirEtapasUseCase garantir;
    @Inject
    QuadroFunilUseCase quadro;
    @Inject
    Clock clock;
    @Inject
    EntityManager em;

    @Test
    @TestSecurity(user = "9c120001-9c12-49c0-89c0-000000000001", roles = "ADMIN")
    @JwtSecurity(claims = {
            @Claim(key = "sub", value = "9c120001-9c12-49c0-89c0-000000000001"),
            @Claim(key = "companyId", value = "funil-t12-admin")
    })
    void adminVeLeadsComDonoESemDonoEOcultaOutroToken() {
        var dono = UUID.fromString("9c000001-9c00-49c0-89c0-000000000011");
        criarUsuario(UUID.randomUUID(), dono);
        var empresa = CompanyId.reconstituir("funil-t12-admin");
        var etapa = etapa(empresa.valor(), "Coluna", TipoEtapa.ABERTO, 1, 0, null);
        var comDono = gravarLead(empresa, "+5511912001201", SituacaoLead.ABERTA, null, false, null, null);
        var semDono = gravarLead(empresa, "+5511912001202", SituacaoLead.ABERTA, null, false, null, null);
        salvarNegocio(comDono.getId(), etapa.getId(), ENTRADA, null, null);
        salvarNegocio(semDono.getId(), etapa.getId(), ENTRADA, null, null);
        distribuicao.salvarDono(new DonoLead(comDono.getId(), empresa.valor(), dono, ENTRADA, false), ENTRADA);
        var outro = gravarOutroToken("funil-t12-admin-outra", "+5511912001203");

        var resposta = consultar(null, null);

        assertEquals(Set.of(etapa.getId().toString()), resposta.etapas());
        assertEquals(Set.of(comDono.getId().toString(), semDono.getId().toString()), resposta.leads());
        assertAusente(resposta, outro);
    }

    @Test
    @TestSecurity(user = "9c120002-9c12-49c0-89c0-000000000002", roles = "SELLER")
    @JwtSecurity(claims = {
            @Claim(key = "sub", value = "9c120002-9c12-49c0-89c0-000000000002"),
            @Claim(key = "companyId", value = "funil-t12-seller")
    })
    void vendedorVeSomenteLeadsDeQueEDono() {
        var vendedor = UUID.fromString("9c120002-9c12-49c0-89c0-000000000002");
        var outroDono = UUID.fromString("9c000002-9c00-49c0-89c0-000000000012");
        criarUsuario(UUID.randomUUID(), vendedor);
        criarUsuario(UUID.randomUUID(), outroDono);
        var empresa = CompanyId.reconstituir("funil-t12-seller");
        var etapa = etapa(empresa.valor(), "Coluna", TipoEtapa.ABERTO, 1, 0, null);
        var proprio = gravarLead(empresa, "+5511912001204", SituacaoLead.ABERTA, null, false, null, null);
        var semDono = gravarLead(empresa, "+5511912001205", SituacaoLead.ABERTA, null, false, null, null);
        var alheio = gravarLead(empresa, "+5511912001206", SituacaoLead.ABERTA, null, false, null, null);
        salvarNegocio(proprio.getId(), etapa.getId(), ENTRADA, null, null);
        salvarNegocio(semDono.getId(), etapa.getId(), ENTRADA, null, null);
        salvarNegocio(alheio.getId(), etapa.getId(), ENTRADA, null, null);
        distribuicao.salvarDono(new DonoLead(proprio.getId(), empresa.valor(), vendedor, ENTRADA, false), ENTRADA);
        distribuicao.salvarDono(new DonoLead(alheio.getId(), empresa.valor(), outroDono, ENTRADA, false), ENTRADA);
        var outro = gravarOutroToken("funil-t12-seller-outra", "+5511912001207");

        var resposta = consultar(null, null);

        assertEquals(Set.of(etapa.getId().toString()), resposta.etapas());
        assertEquals(Set.of(proprio.getId().toString()), resposta.leads());
        assertFalse(resposta.leads().contains(semDono.getId().toString()));
        assertFalse(resposta.leads().contains(alheio.getId().toString()));
        assertFalse(resposta.corpo().contains(semDono.getId().toString()));
        assertAusente(resposta, outro);
    }

    @Test
    @TestSecurity(user = "9c120003-9c12-49c0-89c0-000000000003", roles = "ADMIN")
    @JwtSecurity(claims = {
            @Claim(key = "sub", value = "9c120003-9c12-49c0-89c0-000000000003"),
            @Claim(key = "companyId", value = "funil-t12-excluido")
    })
    void leadExcluidoNaoAparece() {
        var empresa = CompanyId.reconstituir("funil-t12-excluido");
        var etapa = etapa(empresa.valor(), "Coluna", TipoEtapa.ABERTO, 1, 0, null);
        var visivel = gravarLead(empresa, "+5511912001208", SituacaoLead.ABERTA, null, false, null, null);
        var excluido = gravarLead(empresa, "+5511912001209", SituacaoLead.ABERTA,
                OffsetDateTime.parse("2026-04-03T00:00:00Z"), false, null, null);
        assertTrue(excluido.isDeleted());
        salvarNegocio(visivel.getId(), etapa.getId(), ENTRADA, null, null);
        salvarNegocio(excluido.getId(), etapa.getId(), ENTRADA, null, null);

        var resposta = consultar(null, null);

        assertEquals(Set.of(visivel.getId().toString()), resposta.leads());
        assertFalse(resposta.corpo().contains(excluido.getId().toString()));
    }

    @Test
    @TestSecurity(user = "9c120004-9c12-49c0-89c0-000000000004", roles = "ADMIN")
    @JwtSecurity(claims = {
            @Claim(key = "sub", value = "9c120004-9c12-49c0-89c0-000000000004"),
            @Claim(key = "companyId", value = "funil-t12-filtro")
    })
    void filtroPorUfEQuenteRestringeOsDoisECadaOmissaoSoltaOProprio() {
        assertSomenteUfEQuente();

        var empresa = CompanyId.reconstituir("funil-t12-filtro");
        var etapa = etapa(empresa.valor(), "Coluna", TipoEtapa.ABERTO, 1, 0, null);
        var spQuente = noQuadro(empresa, etapa, "+5511912001210", true, "SP");
        var spFrio = noQuadro(empresa, etapa, "+5511912001211", false, "SP");
        var rjQuente = noQuadro(empresa, etapa, "+5511912001212", true, "RJ");
        var spMinusculo = noQuadro(empresa, etapa, "+5511912001213", true, "sp");
        var semUf = noQuadro(empresa, etapa, "+5511912001214", false, null);

        assertEquals(Set.of(spQuente), consultar("SP", true).leads());
        assertFalse(consultar("SP", null).leads().contains(spMinusculo));
        assertEquals(Set.of(spQuente, spFrio), consultar("SP", null).leads());
        assertEquals(Set.of(spMinusculo), consultar("sp", null).leads());
        assertEquals(Set.of(spQuente, rjQuente, spMinusculo), consultar(null, true).leads());
        assertEquals(Set.of(spQuente, spFrio, rjQuente, spMinusculo, semUf), consultar(null, null).leads());
    }

    @Test
    @TestSecurity(user = "9c120005-9c12-49c0-89c0-000000000005", roles = "ADMIN")
    @JwtSecurity(claims = {
            @Claim(key = "sub", value = "9c120005-9c12-49c0-89c0-000000000005"),
            @Claim(key = "companyId", value = "funil-t12-previsto")
    })
    void abertoComProbabilidadeCinquentaPreveCincoMil() {
        var empresa = CompanyId.reconstituir("funil-t12-previsto");
        var etapa = etapa(empresa.valor(), "Coluna", TipoEtapa.ABERTO, 1, 50, null);
        var comCem = gravarLead(empresa, "+5511912001215", SituacaoLead.ABERTA, null, false, null, null);
        var semValor = gravarLead(empresa, "+5511912001216", SituacaoLead.ABERTA, null, false, null, null);
        salvarNegocio(comCem.getId(), etapa.getId(), ENTRADA, new BigDecimal("100.00"), null);
        salvarNegocio(semValor.getId(), etapa.getId(), ENTRADA, null, null);
        fresco();
        assertEquals(50, etapas.buscarPorId(etapa.getId(), empresa).orElseThrow().probabilidade().valor());

        var resposta = consultar(null, null);

        assertEquals(Set.of(comCem.getId().toString(), semValor.getId().toString()), resposta.leads());
        var valor = assertColuna(resposta, etapa.getId(), 2, "5000.00");
        assertTrue(valor.compareTo(new BigDecimal("50.00")) != 0, valor.toPlainString());
    }

    @Test
    @TestSecurity(user = "9c120006-9c12-49c0-89c0-000000000006", roles = "ADMIN")
    @JwtSecurity(claims = {
            @Claim(key = "sub", value = "9c120006-9c12-49c0-89c0-000000000006"),
            @Claim(key = "companyId", value = "funil-t12-ganho-par")
    })
    void ganhoSomaValoresFechados() {
        var empresa = CompanyId.reconstituir("funil-t12-ganho-par");
        var ganho = etapa(empresa.valor(), "Fechado", TipoEtapa.GANHO, 1, null, null);
        var dez = gravarLead(empresa, "+5511912001217", SituacaoLead.GANHO, null, false, null, null);
        var quarenta = gravarLead(empresa, "+5511912001218", SituacaoLead.GANHO, null, false, null, null);
        salvarNegocio(dez.getId(), ganho.getId(), ENTRADA, null, new BigDecimal("10.00"));
        salvarNegocio(quarenta.getId(), ganho.getId(), ENTRADA, null, new BigDecimal("40.00"));

        var resposta = consultar(null, null);

        assertEquals(Set.of(dez.getId().toString(), quarenta.getId().toString()), resposta.leads());
        assertColuna(resposta, ganho.getId(), 2, "50.00");
    }

    @Test
    @TestSecurity(user = "9c120007-9c12-49c0-89c0-000000000007", roles = "ADMIN")
    @JwtSecurity(claims = {
            @Claim(key = "sub", value = "9c120007-9c12-49c0-89c0-000000000007"),
            @Claim(key = "companyId", value = "funil-t12-ganho-vazio")
    })
    void ganhoComDezEOutroSemValorFechadoSomaDez() {
        var empresa = CompanyId.reconstituir("funil-t12-ganho-vazio");
        esvaziarPendencias();
        etapa(empresa.valor(), "Aberto", TipoEtapa.ABERTO, 1, 0, null);
        var ganho = etapa(empresa.valor(), "Fechado", TipoEtapa.GANHO, 2, null, null);
        etapa(empresa.valor(), "Perdido", TipoEtapa.PERDIDO, 3, null, null);
        var comDez = gravarLead(empresa, "+5511912001219", SituacaoLead.GANHO, null, false, null, null);
        salvarNegocio(comDez.getId(), ganho.getId(), ENTRADA, null, new BigDecimal("10.00"));
        var semValor = gravarLead(empresa, "+5511912001220", SituacaoLead.GANHO, null, false, null, null);
        assertTrue(colocar.execute(new ColocarLeadsJaGravadosCommand()).isOk());
        fresco();
        var colocado = negocios.buscar(semValor.getId(), empresa).orElseThrow();
        assertEquals(ganho.getId(), colocado.etapaId());
        assertNull(colocado.valorFechado());

        var resposta = consultar(null, null);

        assertEquals(Set.of(comDez.getId().toString(), semValor.getId().toString()),
                resposta.leadsDaColuna(ganho.getId()));
        assertColuna(resposta, ganho.getId(), 2, "10.00");
    }

    @Test
    @TestSecurity(user = "9c120008-9c12-49c0-89c0-000000000008", roles = "ADMIN")
    @JwtSecurity(claims = {
            @Claim(key = "sub", value = "9c120008-9c12-49c0-89c0-000000000008"),
            @Claim(key = "companyId", value = "funil-t12-perdido")
    })
    void perdidoSomaEstimadoSemUsarProbabilidade() {
        var empresa = CompanyId.reconstituir("funil-t12-perdido");
        var perdido = etapa(empresa.valor(), "Perdido", TipoEtapa.PERDIDO, 1, null, null);
        var comDez = gravarLead(empresa, "+5511912001221", SituacaoLead.PERDIDO, null, false, null, null);
        var semValor = gravarLead(empresa, "+5511912001222", SituacaoLead.PERDIDO, null, false, null, null);
        salvarNegocio(comDez.getId(), perdido.getId(), ENTRADA, new BigDecimal("10.00"), null);
        salvarNegocio(semValor.getId(), perdido.getId(), ENTRADA, null, null);
        gravarProbabilidade(perdido, empresa, 50);
        assertEquals(50, etapas.buscarPorId(perdido.getId(), empresa).orElseThrow().probabilidade().valor());

        var resposta = consultar(null, null);

        assertEquals(Set.of(comDez.getId().toString(), semValor.getId().toString()), resposta.leads());
        var valor = assertColuna(resposta, perdido.getId(), 2, "10.00");
        assertTrue(valor.compareTo(new BigDecimal("500.00")) != 0, valor.toPlainString());
    }

    @Test
    @TestSecurity(user = "9c120009-9c12-49c0-89c0-000000000009", roles = "ADMIN")
    @JwtSecurity(claims = {
            @Claim(key = "sub", value = "9c120009-9c12-49c0-89c0-000000000009"),
            @Claim(key = "companyId", value = "funil-t12-nascida")
    })
    void abertoNascidaSoPreveDepoisDaProbabilidadeEFinaisNaoMultiplicam() {
        var empresa = CompanyId.reconstituir("funil-t12-nascida");
        assertTrue(garantir.garantirAusentes(empresa).isOk());
        fresco();
        var lista = etapas.listarPorEmpresa(empresa);
        assertEquals(3, lista.size());
        var aberto = unica(lista, TipoEtapa.ABERTO);
        var ganho = unica(lista, TipoEtapa.GANHO);
        var perdido = unica(lista, TipoEtapa.PERDIDO);
        assertEquals(0, aberto.probabilidade().valor());
        assertNull(ganho.probabilidade());
        assertNull(perdido.probabilidade());
        assertTrue(aberto.sla().vazio());

        var leadAberto = gravarLead(empresa, "+5511912001223", SituacaoLead.ABERTA, null, false, null, null);
        var ganhoDez = gravarLead(empresa, "+5511912001224", SituacaoLead.GANHO, null, false, null, null);
        var ganhoVazio = gravarLead(empresa, "+5511912001232", SituacaoLead.GANHO, null, false, null, null);
        var perdidoDez = gravarLead(empresa, "+5511912001225", SituacaoLead.PERDIDO, null, false, null, null);
        var perdidoVazio = gravarLead(empresa, "+5511912001233", SituacaoLead.PERDIDO, null, false, null, null);
        salvarNegocio(leadAberto.getId(), aberto.getId(), ENTRADA, new BigDecimal("100.00"), null);
        salvarNegocio(ganhoDez.getId(), ganho.getId(), ENTRADA, null, new BigDecimal("10.00"));
        salvarNegocio(ganhoVazio.getId(), ganho.getId(), ENTRADA, null, null);
        salvarNegocio(perdidoDez.getId(), perdido.getId(), ENTRADA, new BigDecimal("10.00"), null);
        salvarNegocio(perdidoVazio.getId(), perdido.getId(), ENTRADA, null, null);

        var antes = consultar(null, null);
        assertColuna(antes, aberto.getId(), 1, "0.00");
        assertColuna(antes, ganho.getId(), 2, "10.00");
        assertColuna(antes, perdido.getId(), 2, "10.00");

        gravarProbabilidade(aberto, empresa, 50);
        assertEquals(50, etapas.buscarPorId(aberto.getId(), empresa).orElseThrow().probabilidade().valor());

        var depois = consultar(null, null);
        var previsto = assertColuna(depois, aberto.getId(), 1, "5000.00");
        assertTrue(previsto.compareTo(new BigDecimal("50.00")) != 0, previsto.toPlainString());
        var somaGanho = assertColuna(depois, ganho.getId(), 2, "10.00");
        var somaPerdido = assertColuna(depois, perdido.getId(), 2, "10.00");
        assertTrue(somaGanho.compareTo(new BigDecimal("0.00")) != 0);
        assertTrue(somaPerdido.compareTo(new BigDecimal("500.00")) != 0);
    }

    @Test
    @TestSecurity(user = "9c12000a-9c12-49c0-89c0-00000000000a", roles = "ADMIN")
    @JwtSecurity(claims = {
            @Claim(key = "sub", value = "9c12000a-9c12-49c0-89c0-00000000000a"),
            @Claim(key = "companyId", value = "funil-t12-atraso")
    })
    void cardPassadoDeQuarentaEOitoHorasAtrasaEAindaMudaDeEtapa() {
        var empresa = CompanyId.reconstituir("funil-t12-atraso");
        var comPrazo = etapa(empresa.valor(), "Com prazo", TipoEtapa.ABERTO, 1, 0, 2);
        var semPrazo = Etapa.nascida(empresa, TipoEtapa.ABERTO, OrdemEtapa.tryCreate(2).getValue());
        etapas.salvar(semPrazo);
        var destino = etapa(empresa.valor(), "Proposta", TipoEtapa.ABERTO, 3, 0, null);
        var entrada = clock.instant().minus(Duration.ofHours(48)).minus(Duration.ofSeconds(2));
        var atrasado = gravarLead(empresa, "+5511912001226", SituacaoLead.ABERTA, null, false, null, null);
        var noPrazo = gravarLead(empresa, "+5511912001227", SituacaoLead.ABERTA, null, false, null, null);
        salvarNegocio(atrasado.getId(), comPrazo.getId(), entrada, null, null);
        salvarNegocio(noPrazo.getId(), semPrazo.getId(), entrada, null, null);
        fresco();
        assertEquals(2, etapas.buscarPorId(comPrazo.getId(), empresa).orElseThrow().sla().valor());
        assertTrue(etapas.buscarPorId(semPrazo.getId(), empresa).orElseThrow().sla().vazio());

        var resposta = consultar(null, null);

        assertTrue(resposta.item(atrasado.getId()).get("atrasado").asBoolean());
        assertFalse(resposta.item(noPrazo.getId()).get("atrasado").asBoolean());

        given().contentType(ContentType.JSON)
                .body("{\"etapaId\":\"" + destino.getId() + "\"}")
                .when().post("/funil/leads/" + atrasado.getId() + "/mover")
                .then().statusCode(204);
        fresco();
        assertEquals(destino.getId(), negocios.buscar(atrasado.getId(), empresa).orElseThrow().etapaId());
    }

    @Test
    @TestSecurity(user = "9c00000b-9c00-49c0-89c0-00000000000b", roles = "ADMIN")
    @JwtSecurity(claims = {
            @Claim(key = "sub", value = "9c00000b-9c00-49c0-89c0-00000000000b"),
            @Claim(key = "companyId", value = "funil-t12-item")
    })
    void respostaNaoTemPaginaEItemNaoTemDadosDeCard() {
        var empresa = CompanyId.reconstituir("funil-t12-item");
        var etapa = etapa(empresa.valor(), "Coluna", TipoEtapa.ABERTO, 1, 0, null);
        var lead = gravarLead(empresa, "+5511912001228", SituacaoLead.ABERTA, null, true, "SP", "Campinas",
                "Ana Quadro", "ana.quadro@funil.test");
        var criado = NegocioFunil.tryCreate(lead.getId(), etapa.getId(), ENTRADA);
        assertTrue(criado.isOk());
        var dados = criado.getValue().comDadosAbertos(true, true, true, true, true, LocalDate.of(2026, 6, 1),
                new BigDecimal("100.00"), "Plano Ouro", LocalDate.of(2026, 7, 1));
        assertTrue(dados.isOk());
        negocios.salvar(dados.getValue());

        var resposta = consultar(null, null);

        assertEquals(Set.of("colunas"), resposta.chavesRaiz());
        assertFalse(resposta.raiz().has("page"));
        assertFalse(resposta.raiz().has("perPage"));
        assertFalse(resposta.raiz().has("pagina"));
        assertFalse(resposta.raiz().has("total"));
        assertFalse(resposta.corpo().contains("Ana Quadro"));
        assertFalse(resposta.corpo().contains("ana.quadro@funil.test"));
        assertFalse(resposta.corpo().contains("Plano Ouro"));
        assertFalse(resposta.corpo().contains("+5511912001228"));
        var coluna = resposta.coluna(etapa.getId());
        assertEquals(Set.of("etapaId", "tipo", "quantidade", "valor", "itens"), chaves(coluna));
        var item = resposta.item(lead.getId());
        assertEquals(lead.getId().toString(), item.get("leadId").asText());
        assertEquals(etapa.getId().toString(), item.get("etapaId").asText());
        assertTrue(item.get("atrasado").isBoolean());
        assertEquals(Set.of("leadId", "etapaId", "atrasado"), chaves(item));
        for (String campo : CAMPOS_AUSENTES_NO_ITEM)
            assertFalse(item.has(campo), campo);
    }

    private String noQuadro(CompanyId empresa, Etapa etapa, String telefone, boolean quente, String uf) {
        var lead = gravarLead(empresa, telefone, SituacaoLead.ABERTA, null, quente, uf, null);
        salvarNegocio(lead.getId(), etapa.getId(), ENTRADA, null, null);
        return lead.getId().toString();
    }

    private Resposta consultar(String uf, Boolean quente) {
        fresco();
        var pedido = given();
        if (uf != null)
            pedido = pedido.queryParam("uf", uf);
        if (quente != null)
            pedido = pedido.queryParam("quente", quente);
        var corpo = pedido.when().get("/funil/quadro").then().statusCode(200).extract().asString();
        return Resposta.ler(corpo);
    }

    private BigDecimal assertColuna(Resposta resposta, UUID etapaId, int quantidade, String valor) {
        var esperado = new BigDecimal(valor);
        var executado = quadro.execute(new QuadroFunilQuery(null, null));
        assertTrue(executado.isOk());
        var colunaDto = colunaDto(executado.getValue(), etapaId);
        assertEquals(quantidade, colunaDto.quantidade(), resposta.corpo());
        assertEquals(0, colunaDto.valor().compareTo(esperado), colunaDto.valor().toPlainString());
        var coluna = resposta.coluna(etapaId);
        assertEquals(quantidade, coluna.get("quantidade").asInt(), resposta.corpo());
        var noJson = new BigDecimal(coluna.get("valor").asText());
        assertEquals(0, noJson.compareTo(esperado), noJson.toPlainString());
        return colunaDto.valor();
    }

    private static QuadroFunilDto.Coluna colunaDto(QuadroFunilDto dto, UUID etapaId) {
        for (QuadroFunilDto.Coluna coluna : dto.colunas()) {
            if (etapaId.equals(coluna.etapaId()))
                return coluna;
        }
        throw new AssertionError("coluna " + etapaId + " ausente no caso de uso");
    }

    private static void assertSomenteUfEQuente() {
        var componentes = java.util.Arrays.stream(QuadroFunilQuery.class.getRecordComponents())
                .map(java.lang.reflect.RecordComponent::getName)
                .toList();
        assertEquals(List.of("uf", "quente"), componentes);
        var query = new ArrayList<String>();
        for (var metodo : QuadroFunilResource.class.getDeclaredMethods()) {
            if (!"quadro".equals(metodo.getName()))
                continue;
            for (var parametro : metodo.getParameters()) {
                var anotacao = parametro.getAnnotation(QueryParam.class);
                if (anotacao != null)
                    query.add(anotacao.value());
            }
        }
        assertEquals(List.of("uf", "quente"), query);
        assertFalse(query.contains("cultura"));
        assertFalse(query.contains("tag"));
        assertFalse(query.contains("follow-up"));
        assertFalse(query.contains("followUp"));
    }

    private static void assertAusente(Resposta resposta, OutroToken outro) {
        assertFalse(resposta.corpo().contains(outro.companyId()));
        assertFalse(resposta.corpo().contains(outro.etapaId().toString()));
        assertFalse(resposta.corpo().contains(outro.motivoId().toString()));
        assertFalse(resposta.corpo().contains(outro.leadId().toString()));
        assertFalse(resposta.corpo().contains(outro.negocioId().toString()));
    }

    private OutroToken gravarOutroToken(String companyId, String telefone) {
        var empresa = CompanyId.reconstituir(companyId);
        var etapa = etapa(companyId, "Alheia", TipoEtapa.ABERTO, 1, 0, null);
        var motivo = motivo(companyId, "Preco alheio");
        var lead = gravarLead(empresa, telefone, SituacaoLead.ABERTA, null, false, "SP", "Campinas");
        var criado = NegocioFunil.tryCreate(lead.getId(), etapa.getId(), ENTRADA);
        assertTrue(criado.isOk());
        var negocio = criado.getValue().comPerdido(etapa.getId(), ENTRADA, motivo.getId(), "observacao alheia");
        negocios.salvar(negocio);
        return new OutroToken(companyId, etapa.getId(), motivo.getId(), lead.getId(), negocio.getId());
    }

    private void salvarNegocio(UUID leadId, UUID etapaId, Instant entrada, BigDecimal estimado, BigDecimal fechado) {
        var criado = NegocioFunil.tryCreate(leadId, etapaId, entrada);
        assertTrue(criado.isOk());
        NegocioFunil negocio = criado.getValue();
        if (estimado != null) {
            var comEstimado = negocio.comDadosAbertos(null, null, null, null, null, null, estimado, null, null);
            assertTrue(comEstimado.isOk());
            negocio = comEstimado.getValue();
        }
        if (fechado != null) {
            var valor = ValorComercial.tryCreate(fechado);
            assertTrue(valor.isOk());
            negocio = negocio.comGanho(etapaId, entrada, null, valor.getValue());
        }
        negocios.salvar(negocio);
    }

    private void gravarProbabilidade(Etapa etapa, CompanyId empresa, int probabilidade) {
        fresco();
        var atual = etapas.buscarPorId(etapa.getId(), empresa).orElseThrow();
        assertTrue(atual.alterar(atual.nome(), atual.ordem().valor(), probabilidade, null, atual.sla().valor()).isOk());
        etapas.salvar(atual);
        fresco();
    }

    private void esvaziarPendencias() {
        var resultado = colocar.execute(new ColocarLeadsJaGravadosCommand());
        assertTrue(resultado.isOk());
    }

    private Etapa etapa(String companyId, String nome, TipoEtapa tipo, int ordem, Integer probabilidade, Integer slaDias) {
        var criada = Etapa.tryCreate(companyId, nome, tipo, ordem, true, probabilidade, null, slaDias, false, false);
        assertTrue(criada.isOk(), criada.isOk() ? "" : criada.getErrors().toString());
        etapas.salvar(criada.getValue());
        return criada.getValue();
    }

    private MotivoPerda motivo(String companyId, String nome) {
        var criado = MotivoPerda.tryCreate(companyId, nome, true);
        assertTrue(criado.isOk());
        motivos.salvar(criado.getValue());
        return criado.getValue();
    }

    private static Etapa unica(List<Etapa> lista, TipoEtapa tipo) {
        Etapa encontrada = null;
        for (Etapa etapa : lista) {
            if (etapa.tipo() != tipo)
                continue;
            assertNull(encontrada);
            encontrada = etapa;
        }
        assertTrue(encontrada != null);
        return encontrada;
    }

    private Lead gravarLead(CompanyId empresa, String telefone, SituacaoLead situacao, OffsetDateTime deletedAt,
                            boolean quente, String uf, String cidade) {
        return gravarLead(empresa, telefone, situacao, deletedAt, quente, uf, cidade, "Ana", null);
    }

    private Lead gravarLead(CompanyId empresa, String telefone, SituacaoLead situacao, OffsetDateTime deletedAt,
                            boolean quente, String uf, String cidade, String nome, String email) {
        var agora = Instant.parse("2026-04-01T12:00:00Z");
        var lead = Lead.reconstituir(UUID.randomUUID(), agora, agora, deletedAt, empresa,
                NomeContato.reconstituir(nome), TelefoneBrasil.reconstituir(telefone), null,
                email == null ? null : EmailContato.reconstituir(email),
                OrigemLead.IMPORTACAO, situacao, uf, cidade, quente);
        leads.salvar(lead);
        return lead;
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

    private void fresco() {
        QuarkusTransaction.requiringNew().run(em::clear);
    }

    private static Set<String> chaves(JsonNode node) {
        var nomes = new HashSet<String>();
        node.fieldNames().forEachRemaining(nomes::add);
        return nomes;
    }

    private record OutroToken(String companyId, UUID etapaId, UUID motivoId, UUID leadId, UUID negocioId) {
    }

    private static final class Resposta {
        private final String corpo;
        private final JsonNode raiz;

        private Resposta(String corpo, JsonNode raiz) {
            this.corpo = corpo;
            this.raiz = raiz;
        }

        static Resposta ler(String corpo) {
            try {
                var mapper = new ObjectMapper();
                mapper.enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
                return new Resposta(corpo, mapper.readTree(corpo));
            } catch (Exception e) {
                throw new AssertionError(corpo, e);
            }
        }

        String corpo() {
            return corpo;
        }

        JsonNode raiz() {
            return raiz;
        }

        Set<String> chavesRaiz() {
            return chaves(raiz);
        }

        Set<String> etapas() {
            var ids = new HashSet<String>();
            for (JsonNode coluna : raiz.get("colunas"))
                ids.add(coluna.get("etapaId").asText());
            return ids;
        }

        Set<String> leads() {
            var ids = new HashSet<String>();
            for (JsonNode coluna : raiz.get("colunas")) {
                for (JsonNode item : coluna.get("itens"))
                    ids.add(item.get("leadId").asText());
            }
            return ids;
        }

        Set<String> leadsDaColuna(UUID etapaId) {
            var ids = new HashSet<String>();
            for (JsonNode item : coluna(etapaId).get("itens"))
                ids.add(item.get("leadId").asText());
            return ids;
        }

        JsonNode coluna(UUID etapaId) {
            for (JsonNode coluna : raiz.get("colunas")) {
                if (etapaId.toString().equals(coluna.get("etapaId").asText()))
                    return coluna;
            }
            throw new AssertionError("coluna " + etapaId + " ausente em " + corpo);
        }

        JsonNode item(UUID leadId) {
            for (JsonNode coluna : raiz.get("colunas")) {
                for (JsonNode item : coluna.get("itens")) {
                    if (leadId.toString().equals(item.get("leadId").asText()))
                        return item;
                }
            }
            throw new AssertionError("lead " + leadId + " ausente em " + corpo);
        }
    }
}
