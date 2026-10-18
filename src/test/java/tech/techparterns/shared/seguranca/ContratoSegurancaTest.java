package tech.techparterns.shared.seguranca;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import tech.techparterns.identidade.suporte.TokenTeste;

/** R-01 / CA-09 / CA-27: falhas de segurança viram 401/403 com ErroResponse, nunca 500. */
@QuarkusTest
class ContratoSegurancaTest {

    private final UUID usuario = UUID.randomUUID();
    private final UUID empresa = UUID.randomUUID();

    @Test
    void anonimoRecebe401ComCorpoPadrao() {
        given().get("/teste-seguranca/admin").then()
                .statusCode(401)
                .body("codigoErro", equalTo("NAO_AUTENTICADO"))
                .body("mensagem", notNullValue());
    }

    @Test
    void papelInsuficienteRecebe403ComCorpoPadrao() {
        given().auth().oauth2(TokenTeste.vendedor(usuario, empresa))
                .get("/teste-seguranca/admin").then()
                .statusCode(403)
                .body("codigoErro", equalTo("ACESSO_NEGADO"));
    }

    @Test
    void papelCorretoRecebe200() {
        given().auth().oauth2(TokenTeste.admin(usuario, empresa))
                .get("/teste-seguranca/admin").then().statusCode(200).body(equalTo("admin"));
        given().auth().oauth2(TokenTeste.dono(usuario))
                .get("/teste-seguranca/dono").then().statusCode(200);
    }

    @Test
    void adminNaoAcessaAreaDoDonoEViceVersa() {
        given().auth().oauth2(TokenTeste.admin(usuario, empresa))
                .get("/teste-seguranca/dono").then().statusCode(403);
        given().auth().oauth2(TokenTeste.dono(usuario))
                .get("/teste-seguranca/admin").then().statusCode(403);
    }

    @Test
    void tokenInvalidoRecebe401() {
        given().auth().oauth2("lixo.lixo.lixo")
                .get("/teste-seguranca/autenticado").then()
                .statusCode(401)
                .body("codigoErro", equalTo("NAO_AUTENTICADO"));
    }

    @Test
    void tenantContextLePapelDeUmTokenReal() {
        given().auth().oauth2(TokenTeste.admin(usuario, empresa))
                .get("/teste-seguranca/papel").then().statusCode(200).body(equalTo("ADMIN"));
        given().auth().oauth2(TokenTeste.dono(usuario))
                .get("/teste-seguranca/papel").then().statusCode(200).body(equalTo("PLATFORM_OWNER"));
    }

    @Test
    void companyIdMinusculoChegaAoTenantContext() {
        given().auth().oauth2(TokenTeste.admin(usuario, empresa))
                .get("/teste-seguranca/empresa").then().statusCode(200).body(equalTo(empresa.toString()));
    }

    @Test
    void jwtDoDonoSemCompanyIdNaoViraErro500() {
        given().auth().oauth2(TokenTeste.dono(usuario))
                .get("/teste-seguranca/empresa").then()
                .statusCode(401)
                .body("codigoErro", equalTo("NAO_AUTENTICADO"));
    }
}
