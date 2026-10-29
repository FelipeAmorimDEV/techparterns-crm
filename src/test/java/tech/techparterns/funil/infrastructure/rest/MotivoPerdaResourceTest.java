package tech.techparterns.funil.infrastructure.rest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.jwt.Claim;
import io.quarkus.test.security.jwt.JwtSecurity;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import tech.techparterns.funil.application.usecase.CadastrarMotivoPerdaUseCase;
import tech.techparterns.funil.domain.model.Etapa;
import tech.techparterns.funil.domain.model.HistoricoEtapa;
import tech.techparterns.funil.domain.model.MotivoPerda;
import tech.techparterns.funil.domain.model.TipoEtapa;
import tech.techparterns.funil.domain.repository.EtapaRepository;
import tech.techparterns.funil.domain.repository.HistoricoEtapaRepository;
import tech.techparterns.funil.domain.repository.MotivoPerdaRepository;
import tech.techparterns.shared.domain.CompanyId;

@QuarkusTest
class MotivoPerdaResourceTest {

    private static final String NOME = "Preco";
    private static final Instant TRANSICAO = Instant.parse("2026-04-01T15:30:00Z");

    @Inject
    MotivoPerdaRepository motivos;
    @Inject
    EtapaRepository etapas;
    @Inject
    HistoricoEtapaRepository historicos;
    @Inject
    EntityManager em;

    @Test
    @TestSecurity(user = "u", roles = "ADMIN")
    @JwtSecurity(claims = @Claim(key = "companyId", value = "funil-t6-unica"))
    void nomeRepetidoNaMesmaEmpresaNaoGravaOSegundo() {
        var empresa = CompanyId.reconstituir("funil-t6-unica");
        var id = criar(NOME, true);

        given().contentType(ContentType.JSON)
                .body(corpo(NOME, true))
                .when().post("/funil/motivos-perda")
                .then().statusCode(400)
                .body("mensagem", is(CadastrarMotivoPerdaUseCase.NOME_REPETIDO));

        fresco();
        var lista = motivos.listarPorEmpresa(empresa);
        assertEquals(1, lista.size());
        assertEquals(id, lista.getFirst().getId());
        assertEquals(NOME, lista.getFirst().nome());
    }

    @Test
    @TestSecurity(user = "u", roles = "ADMIN")
    @JwtSecurity(claims = @Claim(key = "companyId", value = "funil-t6-outra"))
    void mesmoNomeEmOutraEmpresaFicaGravado() {
        var empresa = CompanyId.reconstituir("funil-t6-outra");
        var id = criar(NOME, true);

        fresco();
        var lista = motivos.listarPorEmpresa(empresa);
        assertEquals(1, lista.size());
        assertEquals(id, lista.getFirst().getId());
        assertEquals(NOME, lista.getFirst().nome());
        assertTrue(lista.getFirst().ativo());
    }

    @Test
    @TestSecurity(user = "u", roles = "ADMIN")
    @JwtSecurity(claims = @Claim(key = "companyId", value = "funil-t6-ativo"))
    void adminGravaDesativaEReativa() {
        var empresa = CompanyId.reconstituir("funil-t6-ativo");
        var id = criar(NOME, true);

        fresco();
        assertTrue(motivos.buscarPorId(id, empresa).orElseThrow().ativo());

        given().contentType(ContentType.JSON)
                .body("{\"ativo\":false}")
                .when().patch("/funil/motivos-perda/" + id)
                .then().statusCode(204);

        fresco();
        assertFalse(motivos.buscarPorId(id, empresa).orElseThrow().ativo());
        given().when().get("/funil/motivos-perda")
                .then().statusCode(200)
                .body("find { it.id == '" + id + "' }.nome", equalTo(NOME))
                .body("find { it.id == '" + id + "' }.ativo", equalTo(false));

        given().contentType(ContentType.JSON)
                .body("{\"id\":\"" + UUID.randomUUID() + "\",\"ativo\":true}")
                .when().patch("/funil/motivos-perda/" + id)
                .then().statusCode(204);

        fresco();
        assertTrue(motivos.buscarPorId(id, empresa).orElseThrow().ativo());
        given().when().get("/funil/motivos-perda")
                .then().statusCode(200)
                .body("", hasSize(1))
                .body("[0].id", equalTo(id.toString()))
                .body("[0].ativo", equalTo(true));
    }

    @Test
    @TestSecurity(user = "v", roles = "SELLER")
    @JwtSecurity(claims = @Claim(key = "companyId", value = "funil-t6-seller"))
    void vendedorNaoAlteraMotivo() {
        var empresa = CompanyId.reconstituir("funil-t6-seller");
        var motivo = gravarMotivo(empresa.valor(), NOME, true);

        given().contentType(ContentType.JSON)
                .body(corpo("Outro nome", true))
                .when().post("/funil/motivos-perda")
                .then().statusCode(403);
        given().contentType(ContentType.JSON)
                .body("{\"ativo\":false}")
                .when().patch("/funil/motivos-perda/" + motivo.getId())
                .then().statusCode(403);

        fresco();
        var lista = motivos.listarPorEmpresa(empresa);
        assertEquals(1, lista.size());
        assertEquals(motivo.getId(), lista.getFirst().getId());
        assertEquals(NOME, lista.getFirst().nome());
        assertTrue(lista.getFirst().ativo());

        given().when().get("/funil/motivos-perda")
                .then().statusCode(200)
                .body("", hasSize(1))
                .body("[0].id", equalTo(motivo.getId().toString()))
                .body("[0].nome", equalTo(NOME))
                .body("[0].ativo", equalTo(true));
    }

    @Test
    @TestSecurity(user = "u", roles = "ADMIN")
    @JwtSecurity(claims = @Claim(key = "companyId", value = "funil-t6-lista"))
    void listagemFiltraEmpresaDoToken() {
        var minha = CompanyId.reconstituir("funil-t6-lista");
        var outra = CompanyId.reconstituir("funil-t6-lista-alheia");
        var alheio = gravarMotivo(outra.valor(), "So da outra", true);
        var meu = criar("So da minha", false);

        fresco();
        given().when().get("/funil/motivos-perda")
                .then().statusCode(200)
                .body("", hasSize(1))
                .body("[0].id", equalTo(meu.toString()))
                .body("[0].nome", equalTo("So da minha"))
                .body("[0].ativo", equalTo(false))
                .body("id", hasItem(meu.toString()))
                .body("id", not(hasItem(alheio.getId().toString())));

        assertEquals(1, motivos.listarPorEmpresa(minha).size());
        assertEquals(meu, motivos.listarPorEmpresa(minha).getFirst().getId());
        assertEquals(1, motivos.listarPorEmpresa(outra).size());
        assertEquals(alheio.getId(), motivos.listarPorEmpresa(outra).getFirst().getId());
    }

    @Test
    @TestSecurity(user = "u", roles = "ADMIN")
    @JwtSecurity(claims = @Claim(key = "companyId", value = "funil-t6-hist"))
    void desativarNaoApagaHistoricoDeEtapa() {
        var empresa = CompanyId.reconstituir("funil-t6-hist");
        var etapa = gravarEtapa(empresa.valor());
        var usuario = UUID.randomUUID();
        var historico = gravarHistorico(etapa.getId(), usuario);
        var id = criar(NOME, true);

        given().contentType(ContentType.JSON)
                .body("{\"ativo\":false}")
                .when().patch("/funil/motivos-perda/" + id)
                .then().statusCode(204);

        fresco();
        assertFalse(motivos.buscarPorId(id, empresa).orElseThrow().ativo());
        var lido = historicos.buscarPorId(historico.getId(), empresa).orElseThrow();
        assertEquals(historico.getId(), lido.getId());
        assertNull(lido.origemEtapaId());
        assertEquals(etapa.getId(), lido.destinoEtapaId());
        assertEquals(usuario, lido.userId());
        assertEquals(TRANSICAO.toEpochMilli(), lido.quando().toEpochMilli());
    }

    @Test
    @TestSecurity(user = "u", roles = "ADMIN")
    @JwtSecurity(claims = @Claim(key = "companyId", value = "funil-t6-invalido"))
    void nomeEmBrancoEAtivoOmitidoSaoRecusados() {
        var empresa = CompanyId.reconstituir("funil-t6-invalido");

        given().contentType(ContentType.JSON)
                .body("{\"nome\":\"   \",\"ativo\":true}")
                .when().post("/funil/motivos-perda")
                .then().statusCode(400)
                .body("mensagem", is(MotivoPerda.NOME_OBRIGATORIO));
        given().contentType(ContentType.JSON)
                .body("{\"nome\":\"Sem flag\"}")
                .when().post("/funil/motivos-perda")
                .then().statusCode(400)
                .body("mensagem", is(CadastrarMotivoPerdaUseCase.ATIVO_OBRIGATORIO));

        fresco();
        assertTrue(motivos.listarPorEmpresa(empresa).isEmpty());
    }

    private UUID criar(String nome, boolean ativo) {
        String id = given().contentType(ContentType.JSON)
                .body(corpo(nome, ativo))
                .when().post("/funil/motivos-perda")
                .then().statusCode(201)
                .extract().path("id");
        return UUID.fromString(id);
    }

    private static String corpo(String nome, boolean ativo) {
        return "{\"nome\":\"" + nome + "\",\"ativo\":" + ativo + "}";
    }

    private MotivoPerda gravarMotivo(String companyId, String nome, boolean ativo) {
        var motivo = MotivoPerda.tryCreate(companyId, nome, ativo).getValue();
        motivos.salvar(motivo);
        return motivo;
    }

    private Etapa gravarEtapa(String companyId) {
        var etapa = Etapa.tryCreate(companyId, "Contato", TipoEtapa.ABERTO, 1, true,
                null, null, null, false, false).getValue();
        etapas.salvar(etapa);
        return etapa;
    }

    private HistoricoEtapa gravarHistorico(UUID destino, UUID userId) {
        var historico = HistoricoEtapa.tryCreate(null, destino, userId, TRANSICAO).getValue();
        historicos.salvar(historico);
        return historico;
    }

    private void fresco() {
        QuarkusTransaction.requiringNew().run(em::clear);
    }
}
