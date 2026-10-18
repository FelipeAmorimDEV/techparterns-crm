package tech.techparterns.distribuicao.infrastructure.rest;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.jwt.Claim;
import io.quarkus.test.security.jwt.JwtSecurity;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import tech.techparterns.distribuicao.PerfilEmailFalha;
import tech.techparterns.distribuicao.application.TrocarDono;
import tech.techparterns.distribuicao.domain.model.RegraAtribuicao;
import tech.techparterns.distribuicao.domain.repository.DistribuicaoRepository;
import tech.techparterns.identidade.infrastructure.persistence.EmpresaEntity;
import tech.techparterns.identidade.infrastructure.persistence.UsuarioEmpresaEntity;
import tech.techparterns.lead.domain.model.*;
import tech.techparterns.lead.domain.repository.LeadRepository;
import tech.techparterns.shared.domain.CompanyId;

@QuarkusTest
@TestProfile(PerfilEmailFalha.class)
class ParadoFalhaEmailTest {

    @Inject LeadRepository leads;
    @Inject DistribuicaoRepository distribuicao;
    @Inject TrocarDono trocar;
    @Inject EntityManager em;

    @Test
    @TestSecurity(user = "19191919-1919-4191-8191-191919191901", roles = "ADMIN")
    @JwtSecurity(claims = @Claim(key = "companyId", value = "19191919-1919-4191-8191-191919191910"))
    void falhaDefinitivaNaoDesfazAVoltaAFila() {
        var empresa = UUID.fromString("19191919-1919-4191-8191-191919191910");
        var admin = UUID.fromString("19191919-1919-4191-8191-191919191901");
        var vendedor = UUID.fromString("19191919-1919-4191-8191-191919191902");
        QuarkusTransaction.requiringNew().run(() -> {
            var e = new EmpresaEntity();
            e.id = empresa;
            e.nome = "Falha";
            e.status = "ACTIVE";
            e.createdAt = Instant.now();
            e.updatedAt = e.createdAt;
            em.persist(e);
            var u = new UsuarioEmpresaEntity();
            u.id = vendedor;
            u.empresaId = empresa;
            u.nome = "Vend";
            u.email = "falha@ca19.test";
            u.papel = "SELLER";
            u.status = "ACTIVE";
            u.createdAt = Instant.now();
            u.updatedAt = u.createdAt;
            em.persist(u);
            var adm = new UsuarioEmpresaEntity();
            adm.id = admin;
            adm.empresaId = empresa;
            adm.nome = "Adm";
            adm.email = "adm@ca19.test";
            adm.papel = "ADMIN";
            adm.status = "ACTIVE";
            adm.createdAt = Instant.now();
            adm.updatedAt = adm.createdAt;
            em.persist(adm);
        });
        var lead = Lead.reconstituir(UUID.randomUUID(), Instant.now(), Instant.now(), null, CompanyId.reconstituir(empresa.toString()),
                NomeContato.reconstituir("Parado"), TelefoneBrasil.reconstituir("+5511988887777"), null,
                EmailContato.reconstituir("p@lead.test"), OrigemLead.IMPORTACAO, SituacaoLead.ABERTA, null, null, false);
        leads.salvar(lead);
        trocar.trocar(empresa.toString(), lead.getId(), vendedor, RegraAtribuicao.MANUAL, null, admin);
        QuarkusTransaction.requiringNew().run(() -> em.createNativeQuery(
                        "update lead_ownership set atribuido_em = :quando where lead_id = :id")
                .setParameter("quando", Instant.now().minus(Duration.ofHours(130)))
                .setParameter("id", lead.getId()).executeUpdate());
        given().contentType(ContentType.JSON)
                .body("{\"automatica\":false,\"diasParaParado\":5,\"devolverParadoAFila\":true}")
                .when().put("/distribuicao/configuracao").then().statusCode(204);
        given().when().post("/distribuicao/parados/verificacao").then().statusCode(200);
        assertNull(distribuicao.buscarDono(empresa.toString(), lead.getId()).orElseThrow().ownerId());
        assertEquals(RegraAtribuicao.PARADO, distribuicao.historico(empresa.toString(), lead.getId())
                .get(distribuicao.historico(empresa.toString(), lead.getId()).size() - 1).regra());
        var status = QuarkusTransaction.requiringNew().call(() -> em.createQuery(
                        "select e.status from EnvioEmailEntity e where e.usuarioId = :u and e.tipo = 'PARADO'", String.class)
                .setParameter("u", vendedor).getSingleResult());
        assertEquals("FALHA", status);
    }
}
