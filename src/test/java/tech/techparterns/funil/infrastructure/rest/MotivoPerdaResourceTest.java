package tech.techparterns.funil.infrastructure.rest;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.jwt.Claim;
import io.quarkus.test.security.jwt.JwtSecurity;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
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

    MotivoPerdaResourceTest() {
    }

    @Test
    @TestSecurity(user="u", roles={"ADMIN"})
    @JwtSecurity(claims={@Claim(key="companyId", value="funil-t6-unica")})
    void nomeRepetidoNaMesmaEmpresaNaoGravaOSegundo() {
        CompanyId empresa = CompanyId.reconstituir((String)"funil-t6-unica");
        UUID id = this.criar(NOME, true);
        ((ValidatableResponse)((ValidatableResponse)((Response)RestAssured.given().contentType(ContentType.JSON).body(MotivoPerdaResourceTest.corpo(NOME, true)).when().post("/funil/motivos-perda", new Object[0])).then()).statusCode(400)).body("mensagem", CoreMatchers.is((Object)"Já existe motivo de perda com esse nome nesta empresa"), new Object[0]);
        this.fresco();
        List lista = this.motivos.listarPorEmpresa(empresa);
        Assertions.assertEquals((int)1, (int)lista.size());
        Assertions.assertEquals((Object)id, (Object)((MotivoPerda)lista.getFirst()).getId());
        Assertions.assertEquals((Object)NOME, (Object)((MotivoPerda)lista.getFirst()).nome());
    }

    @Test
    @TestSecurity(user="u", roles={"ADMIN"})
    @JwtSecurity(claims={@Claim(key="companyId", value="funil-t6-outra")})
    void mesmoNomeEmOutraEmpresaFicaGravado() {
        CompanyId empresa = CompanyId.reconstituir((String)"funil-t6-outra");
        UUID id = this.criar(NOME, true);
        this.fresco();
        List lista = this.motivos.listarPorEmpresa(empresa);
        Assertions.assertEquals((int)1, (int)lista.size());
        Assertions.assertEquals((Object)id, (Object)((MotivoPerda)lista.getFirst()).getId());
        Assertions.assertEquals((Object)NOME, (Object)((MotivoPerda)lista.getFirst()).nome());
        Assertions.assertTrue((boolean)((MotivoPerda)lista.getFirst()).ativo());
    }

    @Test
    @TestSecurity(user="u", roles={"ADMIN"})
    @JwtSecurity(claims={@Claim(key="companyId", value="funil-t6-ativo")})
    void adminGravaDesativaEReativa() {
        CompanyId empresa = CompanyId.reconstituir((String)"funil-t6-ativo");
        UUID id = this.criar(NOME, true);
        this.fresco();
        Assertions.assertTrue((boolean)((MotivoPerda)this.motivos.buscarPorId(id, empresa).orElseThrow()).ativo());
        ((ValidatableResponse)((Response)RestAssured.given().contentType(ContentType.JSON).body("{\"ativo\":false}").when().patch("/funil/motivos-perda/" + String.valueOf(id), new Object[0])).then()).statusCode(204);
        this.fresco();
        Assertions.assertFalse((boolean)((MotivoPerda)this.motivos.buscarPorId(id, empresa).orElseThrow()).ativo());
        ((ValidatableResponse)((ValidatableResponse)((ValidatableResponse)((Response)RestAssured.given().when().get("/funil/motivos-perda", new Object[0])).then()).statusCode(200)).body("find { it.id == '" + String.valueOf(id) + "' }.nome", Matchers.equalTo((Object)NOME), new Object[0])).body("find { it.id == '" + String.valueOf(id) + "' }.ativo", Matchers.equalTo((Object)false), new Object[0]);
        ((ValidatableResponse)((Response)RestAssured.given().contentType(ContentType.JSON).body("{\"id\":\"" + String.valueOf(UUID.randomUUID()) + "\",\"ativo\":true}").when().patch("/funil/motivos-perda/" + String.valueOf(id), new Object[0])).then()).statusCode(204);
        this.fresco();
        Assertions.assertTrue((boolean)((MotivoPerda)this.motivos.buscarPorId(id, empresa).orElseThrow()).ativo());
        ((ValidatableResponse)((ValidatableResponse)((ValidatableResponse)((ValidatableResponse)((Response)RestAssured.given().when().get("/funil/motivos-perda", new Object[0])).then()).statusCode(200)).body("", Matchers.hasSize((int)1), new Object[0])).body("[0].id", Matchers.equalTo((Object)id.toString()), new Object[0])).body("[0].ativo", Matchers.equalTo((Object)true), new Object[0]);
    }

    @Test
    @TestSecurity(user="v", roles={"SELLER"})
    @JwtSecurity(claims={@Claim(key="companyId", value="funil-t6-seller")})
    void vendedorNaoAlteraMotivo() {
        CompanyId empresa = CompanyId.reconstituir((String)"funil-t6-seller");
        MotivoPerda motivo = this.gravarMotivo(empresa.valor(), NOME, true);
        ((ValidatableResponse)((Response)RestAssured.given().contentType(ContentType.JSON).body(MotivoPerdaResourceTest.corpo("Outro nome", true)).when().post("/funil/motivos-perda", new Object[0])).then()).statusCode(403);
        ((ValidatableResponse)((Response)RestAssured.given().contentType(ContentType.JSON).body("{\"ativo\":false}").when().patch("/funil/motivos-perda/" + String.valueOf(motivo.getId()), new Object[0])).then()).statusCode(403);
        this.fresco();
        List lista = this.motivos.listarPorEmpresa(empresa);
        Assertions.assertEquals((int)1, (int)lista.size());
        Assertions.assertEquals((Object)motivo.getId(), (Object)((MotivoPerda)lista.getFirst()).getId());
        Assertions.assertEquals((Object)NOME, (Object)((MotivoPerda)lista.getFirst()).nome());
        Assertions.assertTrue((boolean)((MotivoPerda)lista.getFirst()).ativo());
        ((ValidatableResponse)((ValidatableResponse)((ValidatableResponse)((ValidatableResponse)((ValidatableResponse)((Response)RestAssured.given().when().get("/funil/motivos-perda", new Object[0])).then()).statusCode(200)).body("", Matchers.hasSize((int)1), new Object[0])).body("[0].id", Matchers.equalTo((Object)motivo.getId().toString()), new Object[0])).body("[0].nome", Matchers.equalTo((Object)NOME), new Object[0])).body("[0].ativo", Matchers.equalTo((Object)true), new Object[0]);
    }

    @Test
    @TestSecurity(user="u", roles={"ADMIN"})
    @JwtSecurity(claims={@Claim(key="companyId", value="funil-t6-lista")})
    void listagemFiltraEmpresaDoToken() {
        CompanyId minha = CompanyId.reconstituir((String)"funil-t6-lista");
        CompanyId outra = CompanyId.reconstituir((String)"funil-t6-lista-alheia");
        MotivoPerda alheio = this.gravarMotivo(outra.valor(), "So da outra", true);
        UUID meu = this.criar("So da minha", false);
        this.fresco();
        ((ValidatableResponse)((ValidatableResponse)((ValidatableResponse)((ValidatableResponse)((ValidatableResponse)((ValidatableResponse)((ValidatableResponse)((Response)RestAssured.given().when().get("/funil/motivos-perda", new Object[0])).then()).statusCode(200)).body("", Matchers.hasSize((int)1), new Object[0])).body("[0].id", Matchers.equalTo((Object)meu.toString()), new Object[0])).body("[0].nome", Matchers.equalTo((Object)"So da minha"), new Object[0])).body("[0].ativo", Matchers.equalTo((Object)false), new Object[0])).body("id", Matchers.hasItem((Object)meu.toString()), new Object[0])).body("id", Matchers.not((Matcher)Matchers.hasItem((Object)alheio.getId().toString())), new Object[0]);
        Assertions.assertEquals((int)1, (int)this.motivos.listarPorEmpresa(minha).size());
        Assertions.assertEquals((Object)meu, (Object)((MotivoPerda)this.motivos.listarPorEmpresa(minha).getFirst()).getId());
        Assertions.assertEquals((int)1, (int)this.motivos.listarPorEmpresa(outra).size());
        Assertions.assertEquals((Object)alheio.getId(), (Object)((MotivoPerda)this.motivos.listarPorEmpresa(outra).getFirst()).getId());
    }

    @Test
    @TestSecurity(user="u", roles={"ADMIN"})
    @JwtSecurity(claims={@Claim(key="companyId", value="funil-t6-hist")})
    void desativarNaoApagaHistoricoDeEtapa() {
        CompanyId empresa = CompanyId.reconstituir((String)"funil-t6-hist");
        Etapa etapa = this.gravarEtapa(empresa.valor());
        UUID usuario = UUID.randomUUID();
        HistoricoEtapa historico = this.gravarHistorico(etapa.getId(), usuario);
        UUID id = this.criar(NOME, true);
        ((ValidatableResponse)((Response)RestAssured.given().contentType(ContentType.JSON).body("{\"ativo\":false}").when().patch("/funil/motivos-perda/" + String.valueOf(id), new Object[0])).then()).statusCode(204);
        this.fresco();
        Assertions.assertFalse((boolean)((MotivoPerda)this.motivos.buscarPorId(id, empresa).orElseThrow()).ativo());
        HistoricoEtapa lido = (HistoricoEtapa)this.historicos.buscarPorId(historico.getId(), empresa).orElseThrow();
        Assertions.assertEquals((Object)historico.getId(), (Object)lido.getId());
        Assertions.assertNull((Object)lido.origemEtapaId());
        Assertions.assertEquals((Object)etapa.getId(), (Object)lido.destinoEtapaId());
        Assertions.assertEquals((Object)usuario, (Object)lido.userId());
        Assertions.assertEquals((long)TRANSICAO.toEpochMilli(), (long)lido.quando().toEpochMilli());
    }

    @Test
    @TestSecurity(user="u", roles={"ADMIN"})
    @JwtSecurity(claims={@Claim(key="companyId", value="funil-t6-invalido")})
    void nomeEmBrancoEAtivoOmitidoSaoRecusados() {
        CompanyId empresa = CompanyId.reconstituir((String)"funil-t6-invalido");
        ((ValidatableResponse)((ValidatableResponse)((Response)RestAssured.given().contentType(ContentType.JSON).body("{\"nome\":\"   \",\"ativo\":true}").when().post("/funil/motivos-perda", new Object[0])).then()).statusCode(400)).body("mensagem", CoreMatchers.is((Object)"Nome do motivo de perda é obrigatório"), new Object[0]);
        ((ValidatableResponse)((ValidatableResponse)((Response)RestAssured.given().contentType(ContentType.JSON).body("{\"nome\":\"Sem flag\"}").when().post("/funil/motivos-perda", new Object[0])).then()).statusCode(400)).body("mensagem", CoreMatchers.is((Object)"O indicador de ativo do motivo de perda é obrigatório"), new Object[0]);
        this.fresco();
        Assertions.assertTrue((boolean)this.motivos.listarPorEmpresa(empresa).isEmpty());
    }

    private UUID criar(String nome, boolean ativo) {
        String id = (String)((ValidatableResponse)((ValidatableResponse)((Response)RestAssured.given().contentType(ContentType.JSON).body(MotivoPerdaResourceTest.corpo(nome, ativo)).when().post("/funil/motivos-perda", new Object[0])).then()).statusCode(201)).extract().path("id", new String[0]);
        return UUID.fromString(id);
    }

    private static String corpo(String nome, boolean ativo) {
        return "{\"nome\":\"" + nome + "\",\"ativo\":" + ativo + "}";
    }

    private MotivoPerda gravarMotivo(String companyId, String nome, boolean ativo) {
        MotivoPerda motivo = (MotivoPerda)MotivoPerda.tryCreate((String)companyId, (String)nome, (boolean)ativo).getValue();
        this.motivos.salvar(motivo);
        return motivo;
    }

    private Etapa gravarEtapa(String companyId) {
        Etapa etapa = (Etapa)Etapa.tryCreate((String)companyId, (String)"Contato", (TipoEtapa)TipoEtapa.ABERTO, (Integer)1, (boolean)true, null, null, null, (boolean)false, (boolean)false).getValue();
        this.etapas.salvar(etapa);
        return etapa;
    }

    private HistoricoEtapa gravarHistorico(UUID destino, UUID userId) {
        HistoricoEtapa historico = (HistoricoEtapa)HistoricoEtapa.tryCreate(null, (UUID)destino, (UUID)userId, (Instant)TRANSICAO).getValue();
        this.historicos.salvar(historico);
        return historico;
    }

    private void fresco() {
        QuarkusTransaction.requiringNew().run(() -> ((EntityManager)this.em).clear());
    }
}
