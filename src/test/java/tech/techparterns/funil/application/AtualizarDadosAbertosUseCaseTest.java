package tech.techparterns.funil.application;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
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
import tech.techparterns.distribuicao.domain.model.DonoLead;
import tech.techparterns.distribuicao.domain.repository.DistribuicaoRepository;
import tech.techparterns.funil.application.usecase.AtualizarDadosAbertosUseCase;
import tech.techparterns.funil.domain.model.Etapa;
import tech.techparterns.funil.domain.model.NegocioFunil;
import tech.techparterns.funil.domain.model.TextoPlano;
import tech.techparterns.funil.domain.model.TipoEtapa;
import tech.techparterns.funil.domain.model.ValorComercial;
import tech.techparterns.funil.domain.repository.EtapaRepository;
import tech.techparterns.funil.domain.repository.NegocioFunilRepository;
import tech.techparterns.identidade.infrastructure.persistence.EmpresaEntity;
import tech.techparterns.identidade.infrastructure.persistence.UsuarioEmpresaEntity;
import tech.techparterns.lead.domain.model.Lead;
import tech.techparterns.lead.domain.model.NomeContato;
import tech.techparterns.lead.domain.model.TelefoneBrasil;
import tech.techparterns.lead.domain.repository.LeadRepository;
import tech.techparterns.shared.domain.CompanyId;

@QuarkusTest
class AtualizarDadosAbertosUseCaseTest {

    private static final Instant ENTRADA = Instant.parse("2026-04-01T15:30:00Z");
    private static final LocalDate DEMO_ANTERIOR = LocalDate.of(2026, 5, 10);
    private static final LocalDate PREVISAO_ANTERIOR = LocalDate.of(2026, 8, 1);
    private static final String PLANO_ANTERIOR = "Basico";
    private static final String VALOR_ANTERIOR = "25.00";

    @Inject
    EtapaRepository etapas;
    @Inject
    NegocioFunilRepository negocios;
    @Inject
    LeadRepository leads;
    @Inject
    DistribuicaoRepository distribuicao;
    @Inject
    EntityManager em;

    @Test
    @TestSecurity(user = "8a000001-8a00-48a0-88a0-000000000001", roles = "SELLER")
    @JwtSecurity(claims = {
            @Claim(key = "sub", value = "8a000001-8a00-48a0-88a0-000000000001"),
            @Claim(key = "companyId", value = "funil-t8-roteiro")
    })
    void donoGravaTresRespostasEDuasEmBrancoSemMudarEtapa() {
        var dono = UUID.fromString("8a000001-8a00-48a0-88a0-000000000001");
        var montado = montar("funil-t8-roteiro", dono, "+5511910008001");

        patch(montado.leadId(), corpo(true, false, true, null, null, null, null, null, null))
                .statusCode(200);

        assertAberto(montado, true, false, true, null, null, null, null, null, null);
    }

    @Test
    @TestSecurity(user = "8a000002-8a00-48a0-88a0-000000000002", roles = "SELLER")
    @JwtSecurity(claims = {
            @Claim(key = "sub", value = "8a000002-8a00-48a0-88a0-000000000002"),
            @Claim(key = "companyId", value = "funil-t8-comercial")
    })
    void donoGravaValorPlanoPrevisaoEDataDemoSemMudarEtapa() {
        var dono = UUID.fromString("8a000002-8a00-48a0-88a0-000000000002");
        var montado = montar("funil-t8-comercial", dono, "+5511910008002");
        var plano = "  Interesse no plano comercial anual XXXXX  ";
        assertEquals(40, plano.trim().length());

        patch(montado.leadId(), corpo(
                null, null, null, null, null,
                "2026-06-20", "100.00", plano, "2026-12-01"))
                .statusCode(200);

        assertAberto(montado, null, null, null, null, null,
                LocalDate.of(2026, 6, 20), "100.00", plano.trim(), LocalDate.of(2026, 12, 1));
    }

    @Test
    @TestSecurity(user = "8a000003-8a00-48a0-88a0-000000000003", roles = "SELLER")
    @JwtSecurity(claims = {
            @Claim(key = "sub", value = "8a000003-8a00-48a0-88a0-000000000003"),
            @Claim(key = "companyId", value = "funil-t8-negativo")
    })
    void valorNegativoNaoSubstituiOAnterior() {
        var dono = UUID.fromString("8a000003-8a00-48a0-88a0-000000000003");
        var montado = montar("funil-t8-negativo", dono, "+5511910008003");
        gravarAnterior(montado.negocio());

        patch(montado.leadId(), corpo(
                false, true, false, true, true,
                "2026-01-01", "-10.00", "Novo", "2027-01-01"))
                .statusCode(400)
                .body("mensagem", is(ValorComercial.INVALIDO));

        assertAnterior(montado);
    }

    @Test
    @TestSecurity(user = "8a000004-8a00-48a0-88a0-000000000004", roles = "SELLER")
    @JwtSecurity(claims = {
            @Claim(key = "sub", value = "8a000004-8a00-48a0-88a0-000000000004"),
            @Claim(key = "companyId", value = "funil-t8-plano")
    })
    void planoAcimaDeQuarentaSozinhoNaoSubstituiOAnterior() {
        var dono = UUID.fromString("8a000004-8a00-48a0-88a0-000000000004");
        var montado = montar("funil-t8-plano", dono, "+5511910008004");
        gravarAnterior(montado.negocio());
        var longo = " " + "b".repeat(41) + " ";
        assertEquals(41, longo.trim().length());

        patch(montado.leadId(), "{\"planoInteresse\":\"" + longo + "\"}")
                .statusCode(400)
                .body("mensagem", is(TextoPlano.LONGO));

        assertAnterior(montado);
    }

    @Test
    @TestSecurity(user = "8a000005-8a00-48a0-88a0-000000000005", roles = "SELLER")
    @JwtSecurity(claims = {
            @Claim(key = "sub", value = "8a000005-8a00-48a0-88a0-000000000005"),
            @Claim(key = "companyId", value = "funil-t8-previsao")
    })
    void previsaoInvalidaNaoSubstituiAAnterior() {
        var dono = UUID.fromString("8a000005-8a00-48a0-88a0-000000000005");
        var montado = montar("funil-t8-previsao", dono, "+5511910008005");
        gravarAnterior(montado.negocio());

        patch(montado.leadId(), "{\"previsaoFechamento\":\"amanha\"}")
                .statusCode(400)
                .body("mensagem", is(AtualizarDadosAbertosUseCase.PREVISAO_INVALIDA));

        assertAnterior(montado);
    }

    @Test
    @TestSecurity(user = "8a000006-8a00-48a0-88a0-000000000006", roles = "SELLER")
    @JwtSecurity(claims = {
            @Claim(key = "sub", value = "8a000006-8a00-48a0-88a0-000000000006"),
            @Claim(key = "companyId", value = "funil-t8-previsao-vazia")
    })
    void previsaoVaziaGravaAusenteEMantemEtapa() {
        var dono = UUID.fromString("8a000006-8a00-48a0-88a0-000000000006");
        var montado = montar("funil-t8-previsao-vazia", dono, "+5511910008006");
        gravarAnterior(montado.negocio());

        patch(montado.leadId(), corpo(
                true, false, true, null, null,
                "2026-05-10", VALOR_ANTERIOR, PLANO_ANTERIOR, ""))
                .statusCode(200);

        assertAberto(montado, true, false, true, null, null,
                DEMO_ANTERIOR, VALOR_ANTERIOR, PLANO_ANTERIOR, null);
    }

    @Test
    @TestSecurity(user = "8a000007-8a00-48a0-88a0-000000000007", roles = "SELLER")
    @JwtSecurity(claims = {
            @Claim(key = "sub", value = "8a000007-8a00-48a0-88a0-000000000007"),
            @Claim(key = "companyId", value = "funil-t8-nao-dono")
    })
    void vendedorQueNaoEDonoNaoGrava() {
        var dono = UUID.fromString("8a000007-8a00-48a0-88a0-000000000070");
        var montado = montar("funil-t8-nao-dono", dono, "+5511910008007");
        gravarAnterior(montado.negocio());

        patch(montado.leadId(), corpo(
                false, true, false, true, true,
                "2026-01-02", "999.00", "Intruso", "2027-02-02"))
                .statusCode(400)
                .body("mensagem", is(AtualizarDadosAbertosUseCase.NAO_E_DONO));

        assertAnterior(montado);
        assertDono(montado, dono);
    }

    @Test
    @TestSecurity(user = "8a000008-8a00-48a0-88a0-000000000008", roles = "ADMIN")
    @JwtSecurity(claims = {
            @Claim(key = "sub", value = "8a000008-8a00-48a0-88a0-000000000008"),
            @Claim(key = "companyId", value = "funil-t8-admin")
    })
    void adminGravaNoLeadDeOutroVendedorEConservaDono() {
        var dono = UUID.fromString("8a000008-8a00-48a0-88a0-000000000080");
        var montado = montar("funil-t8-admin", dono, "+5511910008008");
        var plano = "  Admin Plan  ";

        patch(montado.leadId(), corpo(
                true, false, true, false, true,
                "2026-09-01", "80.00", plano, "2026-12-15"))
                .statusCode(200);

        assertAberto(montado, true, false, true, false, true,
                LocalDate.of(2026, 9, 1), "80.00", plano.trim(), LocalDate.of(2026, 12, 15));
        assertDono(montado, dono);
    }

    @Test
    @TestSecurity(user = "8a000009-8a00-48a0-88a0-000000000009", roles = "SELLER")
    @JwtSecurity(claims = {
            @Claim(key = "sub", value = "8a000009-8a00-48a0-88a0-000000000009"),
            @Claim(key = "companyId", value = "funil-t8-outra-token")
    })
    void leadDeOutraEmpresaNaoEAlterado() {
        var dono = UUID.fromString("8a000009-8a00-48a0-88a0-000000000009");
        var montado = montar("funil-t8-outra-empresa", dono, "+5511910008009");
        gravarAnterior(montado.negocio());

        patch(montado.leadId(), corpo(
                false, true, false, true, true,
                "2026-03-03", "70.00", "Outra", "2027-03-03"))
                .statusCode(400)
                .body("mensagem", is(AtualizarDadosAbertosUseCase.LEAD_NAO_ENCONTRADO));

        assertAnterior(montado);
    }

    @Test
    @TestSecurity(user = "8a00000a-8a00-48a0-88a0-00000000000a", roles = "SELLER")
    @JwtSecurity(claims = {
            @Claim(key = "sub", value = "8a00000a-8a00-48a0-88a0-00000000000a"),
            @Claim(key = "companyId", value = "funil-t8-excluido")
    })
    void leadExcluidoNaoEAlterado() {
        var dono = UUID.fromString("8a00000a-8a00-48a0-88a0-00000000000a");
        var montado = montar("funil-t8-excluido", dono, "+5511910008010");
        gravarAnterior(montado.negocio());
        montado.lead().excluirLogicamente(OffsetDateTime.parse("2026-04-03T00:00:00Z"));
        leads.salvar(montado.lead());

        patch(montado.leadId(), corpo(
                false, true, false, true, true,
                "2026-04-04", "60.00", "Excluido", "2027-04-04"))
                .statusCode(400)
                .body("mensagem", is(AtualizarDadosAbertosUseCase.LEAD_NAO_ENCONTRADO));

        assertAnterior(montado);
    }

    private Montado montar(String companyId, UUID dono, String telefone) {
        criarUsuario(UUID.randomUUID(), dono);
        var empresa = CompanyId.reconstituir(companyId);
        var lead = gravarLead(empresa, telefone);
        var etapa = gravarEtapa(companyId);
        var negocio = NegocioFunil.tryCreate(lead.getId(), etapa.getId(), ENTRADA);
        assertTrue(negocio.isOk());
        negocios.salvar(negocio.getValue());
        distribuicao.salvarDono(new DonoLead(lead.getId(), companyId, dono, ENTRADA, false), ENTRADA);
        return new Montado(empresa, lead, etapa.getId(), negocio.getValue());
    }

    private void gravarAnterior(NegocioFunil negocio) {
        var atualizado = negocio.comDadosAbertos(
                true, false, true, null, null,
                DEMO_ANTERIOR, new BigDecimal(VALOR_ANTERIOR), PLANO_ANTERIOR, PREVISAO_ANTERIOR);
        assertTrue(atualizado.isOk());
        negocios.salvar(atualizado.getValue());
    }

    private void assertAnterior(Montado montado) {
        assertAberto(montado, true, false, true, null, null,
                DEMO_ANTERIOR, VALOR_ANTERIOR, PLANO_ANTERIOR, PREVISAO_ANTERIOR);
    }

    private void assertAberto(Montado montado, Boolean decisor, Boolean dorPrincipal, Boolean usuariosPrevistos,
                              Boolean prazoDecisao, Boolean orcamento, LocalDate dataDemo, String valor,
                              String plano, LocalDate previsao) {
        fresco();
        var lido = negocios.buscar(montado.leadId(), montado.empresa()).orElseThrow();
        assertEquals(montado.etapaId(), lido.etapaId());
        assertEquals(ENTRADA.toEpochMilli(), lido.entrouNaEtapa().toEpochMilli());
        assertEquals(decisor, lido.decisor());
        assertEquals(dorPrincipal, lido.dorPrincipal());
        assertEquals(usuariosPrevistos, lido.usuariosPrevistos());
        assertEquals(prazoDecisao, lido.prazoDecisao());
        assertEquals(orcamento, lido.orcamento());
        assertEquals(dataDemo, lido.dataDemo());
        if (valor == null) {
            assertNull(lido.valorEstimado());
        } else {
            var esperado = new BigDecimal(valor);
            assertEquals(esperado, lido.valorEstimado().valor());
            assertEquals(esperado.scale(), lido.valorEstimado().valor().scale());
        }
        if (plano == null)
            assertNull(lido.planoInteresse());
        else
            assertEquals(plano, lido.planoInteresse().valor());
        assertEquals(previsao, lido.previsaoFechamento());
    }

    private void assertDono(Montado montado, UUID dono) {
        fresco();
        var posse = distribuicao.buscarDono(montado.empresa().valor(), montado.leadId()).orElseThrow();
        assertEquals(dono, posse.ownerId());
    }

    private static ValidatableResponse patch(UUID leadId, String corpo) {
        return given().contentType(ContentType.JSON)
                .body(corpo)
                .when().patch("/funil/leads/" + leadId)
                .then();
    }

    private static String corpo(Boolean decisor, Boolean dorPrincipal, Boolean usuariosPrevistos,
                                Boolean prazoDecisao, Boolean orcamento, String dataDemo, String valor,
                                String plano, String previsao) {
        return "{"
                + "\"decisor\":" + json(decisor) + ","
                + "\"dorPrincipal\":" + json(dorPrincipal) + ","
                + "\"usuariosPrevistos\":" + json(usuariosPrevistos) + ","
                + "\"prazoDecisao\":" + json(prazoDecisao) + ","
                + "\"orcamento\":" + json(orcamento) + ","
                + "\"dataDemo\":" + json(dataDemo) + ","
                + "\"valorEstimado\":" + (valor == null ? "null" : valor) + ","
                + "\"planoInteresse\":" + json(plano) + ","
                + "\"previsaoFechamento\":" + json(previsao)
                + "}";
    }

    private static String json(Boolean valor) {
        return valor == null ? "null" : valor.toString();
    }

    private static String json(String valor) {
        return valor == null ? "null" : "\"" + valor + "\"";
    }

    private Lead gravarLead(CompanyId empresa, String telefone) {
        var criado = Lead.tryCreate(empresa, NomeContato.reconstituir("Ana"),
                TelefoneBrasil.reconstituir(telefone), null, null);
        assertTrue(criado.isOk());
        leads.salvar(criado.getValue());
        return criado.getValue();
    }

    private Etapa gravarEtapa(String companyId) {
        var etapa = Etapa.tryCreate(companyId, "Aberto", TipoEtapa.ABERTO, 1, true,
                null, null, null, false, false);
        assertTrue(etapa.isOk());
        etapas.salvar(etapa.getValue());
        return etapa.getValue();
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

    private record Montado(CompanyId empresa, Lead lead, UUID etapaId, NegocioFunil negocio) {
        UUID leadId() {
            return lead.getId();
        }
    }
}
