package tech.techparterns.identidade;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.mailer.MockMailbox;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import jakarta.inject.Inject;

import tech.techparterns.identidade.suporte.TokenTeste;

/** Fluxo ponta a ponta: empresa -> setup de senha -> login -> convite -> aceite -> sessão -> autorização. */
@QuarkusTest
class FluxoIdentidadeTest {

    private static final Pattern TOKEN = Pattern.compile("token=([A-Za-z0-9_\\-]+)");
    private static final String ORIGEM = "http://localhost:4200";

    @Inject
    MockMailbox mailbox;

    @BeforeEach
    void limpar() { mailbox.clear(); }

    private String ipUnico() { return "10." + (int) (Math.random() * 250) + "." + (int) (Math.random() * 250) + ".1"; }

    private ValidatableResponse post(String caminho, Object corpo, String ip) {
        return given().contentType(ContentType.JSON).header("X-Forwarded-For", ip).body(corpo)
                .when().post(caminho).then();
    }

    private String tokenDoEmail(String destinatario) throws InterruptedException {
        for (int i = 0; i < 50; i++) {
            var mensagens = mailbox.getMailsSentTo(destinatario);
            if (!mensagens.isEmpty()) {
                Matcher m = TOKEN.matcher(mensagens.get(mensagens.size() - 1).getText());
                assertTrue(m.find());
                return m.group(1);
            }
            Thread.sleep(100);
        }
        return fail("e-mail não enviado para " + destinatario);
    }

    @Test
    void fluxoCompleto() throws Exception {
        String ip = ipUnico();
        String emailAdmin = "admin-" + UUID.randomUUID() + "@empresa.test";
        String dono = TokenTeste.dono(UUID.randomUUID());

        // dono cria empresa
        given().contentType(ContentType.JSON).auth().oauth2(dono)
                .body(java.util.Map.of("nome", "Empresa Teste", "adminNome", "Ana Admin", "adminEmail", emailAdmin))
                .when().post("/plataforma/empresas").then().statusCode(201);

        // login antes de definir senha -> 401 genérico
        post("/auth/login", java.util.Map.of("email", emailAdmin, "senha", "qualquer123"), ip).statusCode(401);

        // admin define a senha pelo link
        String setup = tokenDoEmail(emailAdmin);
        post("/auth/senha/definir", java.util.Map.of("token", setup, "senha", "Senha1234"), ip).statusCode(204);
        post("/auth/senha/definir", java.util.Map.of("token", setup, "senha", "Senha1234"), ip).statusCode(400);

        // login ok, com cookie HttpOnly
        var login = given().contentType(ContentType.JSON).header("X-Forwarded-For", ip)
                .body(java.util.Map.of("email", emailAdmin, "senha", "Senha1234"))
                .when().post("/auth/login").then().statusCode(200)
                .cookie("refresh_token", notNullValue())
                .body("accessToken", notNullValue()).extract();
        String access = login.path("accessToken");
        String refresh = login.cookie("refresh_token");
        assertTrue(login.header("Set-Cookie").contains("HttpOnly"));

        // /me
        given().auth().oauth2(access).when().get("/me").then().statusCode(200).body("email", is(emailAdmin));
        given().when().get("/me").then().statusCode(401);

        // renovar exige Origin permitida e rotaciona
        given().cookie("refresh_token", refresh).header("X-Forwarded-For", ip)
                .when().post("/auth/sessao/renovar").then().statusCode(403);
        var renovada = given().cookie("refresh_token", refresh).header("Origin", ORIGEM).header("X-Forwarded-For", ip)
                .when().post("/auth/sessao/renovar").then().statusCode(200).extract();
        String refresh2 = renovada.cookie("refresh_token");
        assertNotEquals(refresh, refresh2);

        // convite e aceite
        String link = given().auth().oauth2(access).contentType(ContentType.JSON).body("{}")
                .when().post("/convites").then().statusCode(201).extract().path("link");
        Matcher m = TOKEN.matcher(link);
        assertTrue(m.find());
        String emailVendedor = "vend-" + UUID.randomUUID() + "@empresa.test";
        post("/convites/aceitar", java.util.Map.of("token", m.group(1), "email", emailVendedor, "nome", "Vera Vendedora",
                "senha", "Senha1234"), ip).statusCode(201);
        // convite de uso único: segundo aceite falha
        post("/convites/aceitar", java.util.Map.of("token", m.group(1), "email", "outro-" + emailVendedor,
                "nome", "Outro", "senha", "Senha1234"), ip).statusCode(400);

        // vendedor loga, acessa /me, mas não gerencia usuários
        String vend = given().contentType(ContentType.JSON).header("X-Forwarded-For", ip)
                .body(java.util.Map.of("email", emailVendedor, "senha", "Senha1234"))
                .when().post("/auth/login").then().statusCode(200).extract().path("accessToken");
        given().auth().oauth2(vend).when().get("/me").then().statusCode(200);
        given().auth().oauth2(vend).when().get("/usuarios").then().statusCode(403);
        given().auth().oauth2(vend).when().get("/convites").then().statusCode(403);
        given().auth().oauth2(access).when().get("/usuarios").then().statusCode(200);
        given().auth().oauth2(access).when().get("/plataforma/empresas").then().statusCode(403);

        // logout revoga a família
        given().cookie("refresh_token", refresh2).header("Origin", ORIGEM)
                .when().post("/auth/sessao/logout").then().statusCode(204);
        given().cookie("refresh_token", refresh2).header("Origin", ORIGEM).header("X-Forwarded-For", ip)
                .when().post("/auth/sessao/renovar").then().statusCode(401);
    }

    @Test
    void recuperacaoDeSenhaTemRespostaNeutra() {
        String ip = ipUnico();
        post("/auth/senha/recuperar", java.util.Map.of("email", "naoexiste@x.test"), ip).statusCode(202);
    }

    @Test
    void ultimoAdminNaoPodeSerDesativado() throws Exception {
        String ip = ipUnico();
        String email = "solo-" + UUID.randomUUID() + "@empresa.test";
        given().contentType(ContentType.JSON).auth().oauth2(TokenTeste.dono(UUID.randomUUID()))
                .body(java.util.Map.of("nome", "Solo", "adminNome", "Solo Admin", "adminEmail", email))
                .when().post("/plataforma/empresas").then().statusCode(201);
        post("/auth/senha/definir", java.util.Map.of("token", tokenDoEmail(email), "senha", "Senha1234"), ip).statusCode(204);
        var sessao = given().contentType(ContentType.JSON).header("X-Forwarded-For", ip)
                .body(java.util.Map.of("email", email, "senha", "Senha1234"))
                .when().post("/auth/login").then().statusCode(200).extract();
        String access = sessao.path("accessToken");
        String id = given().auth().oauth2(access).when().get("/me").then().extract().path("id");
        given().auth().oauth2(access).when().post("/usuarios/" + id + "/desativar").then().statusCode(400);
    }

    @Test
    void loginBloqueiaAposVariasFalhas() {
        String ip = ipUnico();
        String email = "ninguem-" + UUID.randomUUID() + "@x.test";
        for (int i = 0; i < 6; i++)
            post("/auth/login", java.util.Map.of("email", email, "senha", "errada123"), ip).statusCode(401);
    }
}
