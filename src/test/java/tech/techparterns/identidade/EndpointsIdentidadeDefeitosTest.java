package tech.techparterns.identidade;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.mailer.MockMailbox;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;

import tech.techparterns.identidade.suporte.TokenTeste;

/**
 * Cobre defeitos de contrato nos endpoints de identidade que o fluxo feliz não exercita:
 * corpo nulo, Content-Type ausente em POSTs com corpo opcional, gestão de usuário após cadastro.
 */
@QuarkusTest
class EndpointsIdentidadeDefeitosTest {

    private static final Pattern TOKEN = Pattern.compile("token=([A-Za-z0-9_\\-]+)");
    private static final String ORIGEM = "http://localhost:4200";

    @Inject
    MockMailbox mailbox;

    @BeforeEach
    void limpar() { mailbox.clear(); }

    private String ip() {
        return "10." + (int) (Math.random() * 250) + "." + (int) (Math.random() * 250) + ".1";
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
    void suspenderEReenviarSetupAceitamPostSemContentType() throws Exception {
        String email = "admin-" + UUID.randomUUID() + "@empresa.test";
        String dono = TokenTeste.dono(UUID.randomUUID());
        String empresaId = given().contentType(ContentType.JSON).auth().oauth2(dono)
                .body(Map.of("nome", "Acme", "adminNome", "Admin", "adminEmail", email))
                .when().post("/plataforma/empresas").then().statusCode(201)
                .extract().path("id");

        // corpo opcional: sem Content-Type não pode ser 415
        given().auth().oauth2(dono)
                .when().post("/plataforma/empresas/" + empresaId + "/reenviar-definicao-senha-admin")
                .then().statusCode(anyOf(is(202), is(429)));

        given().auth().oauth2(dono)
                .when().post("/plataforma/empresas/" + empresaId + "/suspender")
                .then().statusCode(204);

        given().auth().oauth2(dono)
                .when().post("/plataforma/empresas/" + empresaId + "/reativar")
                .then().statusCode(204);
    }

    @Test
    void editarEmpresaEUsuarioComCorpoValido() throws Exception {
        String ip = ip();
        String emailAdmin = "admin-" + UUID.randomUUID() + "@empresa.test";
        String emailVend = "vend-" + UUID.randomUUID() + "@empresa.test";
        String dono = TokenTeste.dono(UUID.randomUUID());

        String empresaId = given().contentType(ContentType.JSON).auth().oauth2(dono)
                .body(Map.of("nome", "Empresa", "adminNome", "Ana", "adminEmail", emailAdmin))
                .when().post("/plataforma/empresas").then().statusCode(201)
                .extract().path("id");

        given().contentType(ContentType.JSON).auth().oauth2(dono)
                .body(Map.of("nome", "Empresa Ltda"))
                .when().patch("/plataforma/empresas/" + empresaId)
                .then().statusCode(204);

        given().contentType(ContentType.JSON).header("X-Forwarded-For", ip)
                .body(Map.of("token", tokenDoEmail(emailAdmin), "senha", "Senha1234"))
                .when().post("/auth/senha/definir").then().statusCode(204);

        String access = given().contentType(ContentType.JSON).header("X-Forwarded-For", ip)
                .body(Map.of("email", emailAdmin, "senha", "Senha1234"))
                .when().post("/auth/login").then().statusCode(200)
                .extract().path("accessToken");

        given().contentType(ContentType.JSON).auth().oauth2(access)
                .body(Map.of("nome", "Vendedor", "email", emailVend, "role", "SELLER"))
                .when().post("/usuarios").then().statusCode(202);

        String usuarioId = given().auth().oauth2(access)
                .when().get("/usuarios?status=PENDING&role=SELLER")
                .then().statusCode(200)
                .body("itens.email", hasItem(emailVend))
                .extract().path("itens.find { it.email == '" + emailVend + "' }.id");

        given().contentType(ContentType.JSON).auth().oauth2(access)
                .body(Map.of("nome", "Vendedor Editado", "telefone", "+5511977777777"))
                .when().patch("/usuarios/" + usuarioId)
                .then().statusCode(200)
                .body("nome", is("Vendedor Editado"))
                .body("telefone", is("+5511977777777"));

        given().contentType(ContentType.JSON).auth().oauth2(access)
                .body(Map.of("role", "ADMIN"))
                .when().put("/usuarios/" + usuarioId + "/papel")
                .then().statusCode(204);

        given().contentType(ContentType.JSON).auth().oauth2(access)
                .body(Map.of("nome", "Ana Editada"))
                .when().patch("/me")
                .then().statusCode(200)
                .body("nome", is("Ana Editada"))
                .body("email", is(emailAdmin));
    }

    @Test
    void corpoAusenteNaoDeveVirar500() {
        String ip = ip();
        // login sem corpo: 401 (credenciais), nunca 500
        given().contentType(ContentType.JSON).header("X-Forwarded-For", ip)
                .when().post("/auth/login")
                .then().statusCode(anyOf(is(401), is(400)));

        // recuperação sem corpo: resposta neutra 202
        given().contentType(ContentType.JSON).header("X-Forwarded-For", ip)
                .when().post("/auth/senha/recuperar")
                .then().statusCode(202);

        // definir senha sem corpo: 400
        given().contentType(ContentType.JSON).header("X-Forwarded-For", ip)
                .when().post("/auth/senha/definir")
                .then().statusCode(400);
    }
}
