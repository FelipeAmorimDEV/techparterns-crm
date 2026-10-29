package tech.techparterns.funil.application;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
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
import tech.techparterns.distribuicao.domain.model.DonoLead;
import tech.techparterns.distribuicao.domain.port.FatosLead;
import tech.techparterns.distribuicao.domain.repository.DistribuicaoRepository;
import tech.techparterns.funil.application.command.MarcarGanhoCommand;
import tech.techparterns.funil.application.usecase.MarcarGanhoUseCase;
import tech.techparterns.funil.application.usecase.MarcarPerdidoUseCase;
import tech.techparterns.funil.application.usecase.MoverLeadEtapaUseCase;
import tech.techparterns.funil.domain.model.Etapa;
import tech.techparterns.funil.domain.model.LeadGanho;
import tech.techparterns.funil.domain.model.LeadMudouEtapa;
import tech.techparterns.funil.domain.model.LeadPerdido;
import tech.techparterns.funil.domain.model.MotivoPerda;
import tech.techparterns.funil.domain.model.NegocioFunil;
import tech.techparterns.funil.domain.model.TextoPlano;
import tech.techparterns.funil.domain.model.TipoEtapa;
import tech.techparterns.funil.domain.repository.EtapaRepository;
import tech.techparterns.funil.domain.repository.MotivoPerdaRepository;
import tech.techparterns.funil.domain.repository.NegocioFunilRepository;
import tech.techparterns.funil.infrastructure.persistence.HistoricoEtapaEntity;
import tech.techparterns.identidade.infrastructure.persistence.EmpresaEntity;
import tech.techparterns.identidade.infrastructure.persistence.UsuarioEmpresaEntity;
import tech.techparterns.lead.application.command.ExcluirLeadCommand;
import tech.techparterns.lead.application.usecase.ExcluirLeadUseCase;
import tech.techparterns.lead.domain.model.Lead;
import tech.techparterns.lead.domain.model.NomeContato;
import tech.techparterns.lead.domain.model.OrigemLead;
import tech.techparterns.lead.domain.model.SituacaoLead;
import tech.techparterns.lead.domain.model.TelefoneBrasil;
import tech.techparterns.lead.domain.repository.LeadRepository;
import tech.techparterns.shared.domain.CompanyId;

@QuarkusTest
class MarcarFinalUseCaseTest {

    private static final Instant ENTRADA = Instant.parse("2026-04-01T15:30:00Z");
    private static final String SITUACAO_FINAL = "Situação final não muda";
    private static final String PLANO_UM = "P";
    private static final String PLANO_QUARENTA = "abcdefghijklmnopqrstuvwxyz0123456789ABCD";

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
    FatosLead fatos;
    @Inject
    ExcluirLeadUseCase excluirLead;
    @Inject
    CapturaEventosFunil captura;
    @Inject
    FalhaDuranteGanho falhaDuranteGanho;
    @Inject
    MarcarGanhoUseCase marcarGanho;
    @Inject
    EntityManager em;

    @Test
    @TestSecurity(user = "9c000001-9c00-49c0-89c0-000000000001", roles = "SELLER")
    @JwtSecurity(claims = {
            @Claim(key = "sub", value = "9c000001-9c00-49c0-89c0-000000000001"),
            @Claim(key = "companyId", value = "funil-t10-ganho")
    })
    void ganhoComPlanoDeUmEDeQuarentaGravaFinalESaiDaCarga() {
        assertEquals(1, PLANO_UM.length());
        assertEquals(40, PLANO_QUARENTA.length());
        var dono = UUID.fromString("9c000001-9c00-49c0-89c0-000000000001");
        var cenario = preparar("funil-t10-ganho", dono, true);
        var curto = colocar(cenario, "+5511910011001", SituacaoLead.ABERTA, cenario.abertaId());
        var longo = colocar(cenario, "+5511910011002", SituacaoLead.ABERTA, cenario.abertaId());
        captura.limpar();

        assertEquals(2, carga(cenario.empresa().valor(), dono));
        assertTrue(listadoNaCarteira(cenario.empresa().valor(), dono, curto.getId()));
        assertTrue(listadoNaCarteira(cenario.empresa().valor(), dono, longo.getId()));
        assertFalse(listadoSemDono(cenario.empresa().valor(), curto.getId()));
        assertFalse(listadoSemDono(cenario.empresa().valor(), longo.getId()));

        ganho(curto.getId(), "{\"planoFechado\":\"" + PLANO_UM + "\",\"valorFechado\":1.00}")
                .statusCode(204);

        fresco();
        assertEquals(1, carga(cenario.empresa().valor(), dono));
        assertFalse(listadoNaCarteira(cenario.empresa().valor(), dono, curto.getId()));
        assertTrue(listadoNaCarteira(cenario.empresa().valor(), dono, longo.getId()));
        assertFalse(listadoSemDono(cenario.empresa().valor(), curto.getId()));
        assertEquals(1, captura.ocorrencias(LeadGanho.class));
        assertEquals(0, captura.ocorrencias(LeadMudouEtapa.class));

        ganho(longo.getId(), "{\"planoFechado\":\"" + PLANO_QUARENTA + "\",\"valorFechado\":10.00}")
                .statusCode(204);

        fresco();
        var etapaGanho = unicaAtiva(cenario.empresa(), TipoEtapa.GANHO);
        assertEquals(cenario.ganhoId(), etapaGanho);
        var negocioCurto = negocios.buscar(curto.getId(), cenario.empresa()).orElseThrow();
        var negocioLongo = negocios.buscar(longo.getId(), cenario.empresa()).orElseThrow();
        assertEquals(etapaGanho, negocioCurto.etapaId());
        assertEquals(etapaGanho, negocioLongo.etapaId());
        assertEquals(PLANO_UM, negocioCurto.planoFechado().valor());
        assertEquals(PLANO_QUARENTA, negocioLongo.planoFechado().valor());
        assertEquals(0, new BigDecimal("1.00").compareTo(negocioCurto.valorFechado().valor()));
        assertEquals(0, new BigDecimal("10.00").compareTo(negocioLongo.valorFechado().valor()));
        assertEquals(SituacaoLead.GANHO, leads.buscarPorId(curto.getId(), cenario.empresa()).orElseThrow().situacao());
        assertEquals(SituacaoLead.GANHO, leads.buscarPorId(longo.getId(), cenario.empresa()).orElseThrow().situacao());
        assertEquals(dono, distribuicao.buscarDono(cenario.empresa().valor(), curto.getId()).orElseThrow().ownerId());
        assertEquals(dono, distribuicao.buscarDono(cenario.empresa().valor(), longo.getId()).orElseThrow().ownerId());
        assertHistorico(cenario.abertaId(), etapaGanho, dono, 2);
        assertEquals(0, carga(cenario.empresa().valor(), dono));
        assertFalse(listadoNaCarteira(cenario.empresa().valor(), dono, curto.getId()));
        assertFalse(listadoNaCarteira(cenario.empresa().valor(), dono, longo.getId()));
        assertFalse(listadoSemDono(cenario.empresa().valor(), curto.getId()));
        assertFalse(listadoSemDono(cenario.empresa().valor(), longo.getId()));
        assertEquals(2, captura.eventos().size());
        assertEquals(2, captura.ocorrencias(LeadGanho.class));
        assertEquals(0, captura.ocorrencias(LeadMudouEtapa.class));
        assertEquals(0, captura.ocorrencias(LeadPerdido.class));
    }

    @Test
    @TestSecurity(user = "9c000002-9c00-49c0-89c0-000000000002", roles = "SELLER")
    @JwtSecurity(claims = {
            @Claim(key = "sub", value = "9c000002-9c00-49c0-89c0-000000000002"),
            @Claim(key = "companyId", value = "funil-t10-plano")
    })
    void planoOuValorInvalidoConservaEstadoEPlanoAparadoConclui() {
        var dono = UUID.fromString("9c000002-9c00-49c0-89c0-000000000002");
        var cenario = preparar("funil-t10-plano", dono, true);
        var lead = colocar(cenario, "+5511910011003", SituacaoLead.ABERTA, cenario.abertaId());
        var acima = "  " + "c".repeat(41) + "  ";
        assertEquals(41, acima.trim().length());
        captura.limpar();

        recusarGanho(cenario, lead, "{\"valorFechado\":10.00}", TextoPlano.VAZIO);
        recusarGanho(cenario, lead, "{\"planoFechado\":\"\",\"valorFechado\":10.00}", TextoPlano.VAZIO);
        recusarGanho(cenario, lead, "{\"planoFechado\":\"   \",\"valorFechado\":10.00}", TextoPlano.VAZIO);
        recusarGanho(cenario, lead,
                "{\"planoFechado\":\"" + acima + "\",\"valorFechado\":10.00}", TextoPlano.LONGO);
        recusarGanho(cenario, lead, "{\"planoFechado\":\"ok\"}", MarcarGanhoUseCase.VALOR_FECHADO_NAO_POSITIVO);
        recusarGanho(cenario, lead, "{\"planoFechado\":\"ok\",\"valorFechado\":0}",
                MarcarGanhoUseCase.VALOR_FECHADO_NAO_POSITIVO);
        recusarGanho(cenario, lead, "{\"planoFechado\":\"ok\",\"valorFechado\":-1}",
                MarcarGanhoUseCase.VALOR_FECHADO_NAO_POSITIVO);

        ganho(lead.getId(), "{\"planoFechado\":\"  plano  \",\"valorFechado\":8.00}").statusCode(204);

        fresco();
        var negocio = negocios.buscar(lead.getId(), cenario.empresa()).orElseThrow();
        assertEquals(unicaAtiva(cenario.empresa(), TipoEtapa.GANHO), negocio.etapaId());
        assertEquals("plano", negocio.planoFechado().valor());
        assertEquals(0, new BigDecimal("8.00").compareTo(negocio.valorFechado().valor()));
        assertEquals(SituacaoLead.GANHO, leads.buscarPorId(lead.getId(), cenario.empresa()).orElseThrow().situacao());
        assertEquals(dono, distribuicao.buscarDono(cenario.empresa().valor(), lead.getId()).orElseThrow().ownerId());
        assertHistorico(cenario.abertaId(), cenario.ganhoId(), dono, 1);
        assertEquals(1, captura.eventos().size());
        assertEquals(1, captura.ocorrencias(LeadGanho.class));
        assertEquals(0, captura.ocorrencias(LeadMudouEtapa.class));
        assertEquals(0, captura.ocorrencias(LeadPerdido.class));
    }

    @Test
    @TestSecurity(user = "9c000003-9c00-49c0-89c0-000000000003", roles = "SELLER")
    @JwtSecurity(claims = {
            @Claim(key = "sub", value = "9c000003-9c00-49c0-89c0-000000000003"),
            @Claim(key = "companyId", value = "funil-t10-perdido")
    })
    void perdidoComMotivoAtivoGravaFinalEObservacao() {
        var dono = UUID.fromString("9c000003-9c00-49c0-89c0-000000000003");
        var cenario = preparar("funil-t10-perdido", dono, true);
        var motivo = motivo(cenario.empresa().valor(), "Preco", true);
        var semObservacao = colocar(cenario, "+5511910011004", SituacaoLead.ABERTA, cenario.abertaId());
        var comObservacao = colocar(cenario, "+5511910011005", SituacaoLead.ABERTA, cenario.abertaId());
        var longa = "o".repeat(301);
        assertEquals(301, longa.length());
        captura.limpar();

        perdido(semObservacao.getId(), "{\"motivoPerdaId\":\"" + motivo.getId() + "\"}").statusCode(204);
        perdido(comObservacao.getId(),
                "{\"motivoPerdaId\":\"" + motivo.getId() + "\",\"observacao\":\"" + longa + "\"}")
                .statusCode(204);

        fresco();
        var etapaPerdido = unicaAtiva(cenario.empresa(), TipoEtapa.PERDIDO);
        assertEquals(cenario.perdidoId(), etapaPerdido);
        var omitida = negocios.buscar(semObservacao.getId(), cenario.empresa()).orElseThrow();
        var gravada = negocios.buscar(comObservacao.getId(), cenario.empresa()).orElseThrow();
        assertEquals(etapaPerdido, omitida.etapaId());
        assertEquals(etapaPerdido, gravada.etapaId());
        assertEquals(motivo.getId(), omitida.motivoPerdaId());
        assertEquals(motivo.getId(), gravada.motivoPerdaId());
        assertNull(omitida.observacao());
        assertEquals(longa, gravada.observacao());
        assertEquals(SituacaoLead.PERDIDO,
                leads.buscarPorId(semObservacao.getId(), cenario.empresa()).orElseThrow().situacao());
        assertEquals(SituacaoLead.PERDIDO,
                leads.buscarPorId(comObservacao.getId(), cenario.empresa()).orElseThrow().situacao());
        assertEquals(dono,
                distribuicao.buscarDono(cenario.empresa().valor(), semObservacao.getId()).orElseThrow().ownerId());
        assertEquals(dono,
                distribuicao.buscarDono(cenario.empresa().valor(), comObservacao.getId()).orElseThrow().ownerId());
        assertHistorico(cenario.abertaId(), etapaPerdido, dono, 2);
        assertEquals(2, captura.eventos().size());
        assertEquals(2, captura.ocorrencias(LeadPerdido.class));
        assertEquals(0, captura.ocorrencias(LeadMudouEtapa.class));
        assertEquals(0, captura.ocorrencias(LeadGanho.class));
    }

    @Test
    @TestSecurity(user = "9c000004-9c00-49c0-89c0-000000000004", roles = "SELLER")
    @JwtSecurity(claims = {
            @Claim(key = "sub", value = "9c000004-9c00-49c0-89c0-000000000004"),
            @Claim(key = "companyId", value = "funil-t10-motivo")
    })
    void motivoInativoAusenteOuDeOutraEmpresaNaoGrava() {
        var dono = UUID.fromString("9c000004-9c00-49c0-89c0-000000000004");
        var cenario = preparar("funil-t10-motivo", dono, true);
        var lead = colocar(cenario, "+5511910011006", SituacaoLead.ABERTA, cenario.abertaId());
        var inativo = motivo(cenario.empresa().valor(), "Inativo", false);
        var alheio = motivo("funil-t10-motivo-alheio", "Alheio", true);
        captura.limpar();

        recusarPerdido(cenario, lead, "{\"motivoPerdaId\":\"" + inativo.getId() + "\"}",
                MarcarPerdidoUseCase.MOTIVO_INVALIDO);
        recusarPerdido(cenario, lead, "{}", MarcarPerdidoUseCase.MOTIVO_INVALIDO);
        recusarPerdido(cenario, lead, "{\"motivoPerdaId\":\"" + alheio.getId() + "\"}",
                MarcarPerdidoUseCase.MOTIVO_INVALIDO);
    }

    @Test
    @TestSecurity(user = "9c000005-9c00-49c0-89c0-000000000005", roles = "SELLER")
    @JwtSecurity(claims = {
            @Claim(key = "sub", value = "9c000005-9c00-49c0-89c0-000000000005"),
            @Claim(key = "companyId", value = "funil-t10-final")
    })
    void leadGanhoOuPerdidoNaoVoltaNemTrocaDeFinal() {
        var dono = UUID.fromString("9c000005-9c00-49c0-89c0-000000000005");
        var cenario = preparar("funil-t10-final", dono, true);
        var motivo = motivo(cenario.empresa().valor(), "Preco", true);
        var ganho = colocar(cenario, "+5511910011007", SituacaoLead.GANHO, cenario.ganhoId());
        var perdido = colocar(cenario, "+5511910011008", SituacaoLead.PERDIDO, cenario.perdidoId());
        var corpoGanho = "{\"planoFechado\":\"Plano\",\"valorFechado\":10.00}";
        var corpoPerdido = "{\"motivoPerdaId\":\"" + motivo.getId() + "\"}";
        captura.limpar();

        ganho(ganho.getId(), corpoGanho).statusCode(400).body("mensagem", is(SITUACAO_FINAL));
        perdido(ganho.getId(), corpoPerdido).statusCode(400).body("mensagem", is(SITUACAO_FINAL));
        ganho(perdido.getId(), corpoGanho).statusCode(400).body("mensagem", is(SITUACAO_FINAL));
        perdido(perdido.getId(), corpoPerdido).statusCode(400).body("mensagem", is(SITUACAO_FINAL));

        fresco();
        var negocioGanho = negocios.buscar(ganho.getId(), cenario.empresa()).orElseThrow();
        var negocioPerdido = negocios.buscar(perdido.getId(), cenario.empresa()).orElseThrow();
        assertEquals(cenario.ganhoId(), negocioGanho.etapaId());
        assertEquals(cenario.perdidoId(), negocioPerdido.etapaId());
        assertFalse(cenario.abertaId().equals(negocioGanho.etapaId()));
        assertFalse(cenario.abertaId().equals(negocioPerdido.etapaId()));
        assertNull(negocioGanho.planoFechado());
        assertNull(negocioGanho.valorFechado());
        assertNull(negocioGanho.motivoPerdaId());
        assertNull(negocioPerdido.planoFechado());
        assertNull(negocioPerdido.motivoPerdaId());
        assertNull(negocioPerdido.observacao());
        assertEquals(SituacaoLead.GANHO, leads.buscarPorId(ganho.getId(), cenario.empresa()).orElseThrow().situacao());
        assertEquals(SituacaoLead.PERDIDO,
                leads.buscarPorId(perdido.getId(), cenario.empresa()).orElseThrow().situacao());
        assertEquals(dono, distribuicao.buscarDono(cenario.empresa().valor(), ganho.getId()).orElseThrow().ownerId());
        assertEquals(dono, distribuicao.buscarDono(cenario.empresa().valor(), perdido.getId()).orElseThrow().ownerId());
        assertEquals(0, contarHistorico(cenario.abertaId(), cenario.ganhoId(), cenario.perdidoId()));
        assertSemEvento();
    }

    @Test
    @TestSecurity(user = "9c000006-9c00-49c0-89c0-000000000006", roles = "SELLER")
    @JwtSecurity(claims = {
            @Claim(key = "sub", value = "9c000006-9c00-49c0-89c0-000000000006"),
            @Claim(key = "companyId", value = "funil-t10-nao-dono")
    })
    void vendedorQueNaoEDonoNaoConcluiGanhoNemPerdido() {
        var dono = UUID.fromString("9c000006-9c00-49c0-89c0-000000000060");
        var cenario = preparar("funil-t10-nao-dono", dono, true);
        var lead = colocar(cenario, "+5511910011009", SituacaoLead.ABERTA, cenario.abertaId());
        var motivo = motivo(cenario.empresa().valor(), "Preco", true);
        captura.limpar();

        recusarGanho(cenario, lead, "{\"planoFechado\":\"Plano\",\"valorFechado\":10.00}",
                MoverLeadEtapaUseCase.NAO_E_DONO);
        recusarPerdido(cenario, lead, "{\"motivoPerdaId\":\"" + motivo.getId() + "\"}",
                MoverLeadEtapaUseCase.NAO_E_DONO);
    }

    @Test
    @TestSecurity(user = "9c000007-9c00-49c0-89c0-000000000007", roles = "SELLER")
    @JwtSecurity(claims = {
            @Claim(key = "sub", value = "9c000007-9c00-49c0-89c0-000000000007"),
            @Claim(key = "companyId", value = "funil-t10-sem-dono")
    })
    void leadSemDonoNaoEConcluidoPorVendedor() {
        var cenario = preparar("funil-t10-sem-dono", null, false);
        var lead = colocar(cenario, "+5511910011010", SituacaoLead.ABERTA, cenario.abertaId());
        var motivo = motivo(cenario.empresa().valor(), "Preco", true);
        captura.limpar();

        recusarGanho(cenario, lead, "{\"planoFechado\":\"Plano\",\"valorFechado\":10.00}",
                MoverLeadEtapaUseCase.NAO_E_DONO);
        recusarPerdido(cenario, lead, "{\"motivoPerdaId\":\"" + motivo.getId() + "\"}",
                MoverLeadEtapaUseCase.NAO_E_DONO);
    }

    @Test
    @TestSecurity(user = "9c000008-9c00-49c0-89c0-000000000008", roles = "SELLER")
    @JwtSecurity(claims = {
            @Claim(key = "sub", value = "9c000008-9c00-49c0-89c0-000000000008"),
            @Claim(key = "companyId", value = "funil-t10-token")
    })
    void leadDeOutraEmpresaNaoMuda() {
        var dono = UUID.fromString("9c000008-9c00-49c0-89c0-000000000008");
        var cenario = preparar("funil-t10-alheia", dono, true);
        var lead = colocar(cenario, "+5511910011011", SituacaoLead.ABERTA, cenario.abertaId());
        var motivo = motivo(cenario.empresa().valor(), "Preco", true);
        captura.limpar();

        recusarGanho(cenario, lead, "{\"planoFechado\":\"Plano\",\"valorFechado\":10.00}",
                MoverLeadEtapaUseCase.LEAD_NAO_ENCONTRADO);
        recusarPerdido(cenario, lead, "{\"motivoPerdaId\":\"" + motivo.getId() + "\"}",
                MoverLeadEtapaUseCase.LEAD_NAO_ENCONTRADO);
    }

    @Test
    @TestSecurity(user = "9c000009-9c00-49c0-89c0-000000000009", roles = "ADMIN")
    @JwtSecurity(claims = {
            @Claim(key = "sub", value = "9c000009-9c00-49c0-89c0-000000000009"),
            @Claim(key = "companyId", value = "funil-t10-excluir")
    })
    void leadExcluidoNaoMuda() {
        var admin = UUID.fromString("9c000009-9c00-49c0-89c0-000000000009");
        var cenario = preparar("funil-t10-excluir", admin, true);
        var lead = colocar(cenario, "+5511910011012", SituacaoLead.ABERTA, cenario.abertaId());
        var motivo = motivo(cenario.empresa().valor(), "Preco", true);
        excluirLead.execute(new ExcluirLeadCommand(lead.getId()));
        captura.limpar();

        recusarGanho(cenario, lead, "{\"planoFechado\":\"Plano\",\"valorFechado\":10.00}",
                MoverLeadEtapaUseCase.LEAD_NAO_ENCONTRADO);
        recusarPerdido(cenario, lead, "{\"motivoPerdaId\":\"" + motivo.getId() + "\"}",
                MoverLeadEtapaUseCase.LEAD_NAO_ENCONTRADO);

        fresco();
        var gravado = leads.buscarPorId(lead.getId(), cenario.empresa()).orElseThrow();
        assertEquals(SituacaoLead.ABERTA, gravado.situacao());
        assertTrue(gravado.isDeleted());
    }

    @Test
    @TestSecurity(user = "9c00000a-9c00-49c0-89c0-00000000000a", roles = "SELLER")
    @JwtSecurity(claims = {
            @Claim(key = "sub", value = "9c00000a-9c00-49c0-89c0-00000000000a"),
            @Claim(key = "companyId", value = "funil-t10-rollback")
    })
    void falhaDepoisDaEscritaDesfazSituacaoEEtapa() {
        var dono = UUID.fromString("9c00000a-9c00-49c0-89c0-00000000000a");
        var cenario = preparar("funil-t10-rollback", dono, true);
        var lead = colocar(cenario, "+5511910011013", SituacaoLead.ABERTA, cenario.abertaId());
        falhaDuranteGanho.desligar();
        falhaDuranteGanho.ligar();
        try {
            var erro = assertThrows(RuntimeException.class, () -> marcarGanho.execute(
                    new MarcarGanhoCommand(lead.getId(), "plano", new BigDecimal("10.00"))));
            var achou = false;
            for (Throwable atual = erro; atual != null; atual = atual.getCause()) {
                if (atual.getMessage() != null && atual.getMessage().contains("falha depois da escrita do ganho")) {
                    achou = true;
                    break;
                }
                if (atual.getCause() == atual)
                    break;
            }
            assertTrue(achou, erro.toString());
        } finally {
            falhaDuranteGanho.desligar();
        }

        fresco();
        assertEquals(SituacaoLead.ABERTA, leads.buscarPorId(lead.getId(), cenario.empresa()).orElseThrow().situacao());
        assertEquals(cenario.abertaId(), negocios.buscar(lead.getId(), cenario.empresa()).orElseThrow().etapaId());
        assertEquals(0, contarHistorico(cenario.abertaId(), cenario.ganhoId()));
    }

    private void recusarGanho(Cenario cenario, Lead lead, String json, String mensagem) {
        ganho(lead.getId(), json).statusCode(400).body("mensagem", is(mensagem));
        assertAberto(cenario, lead);
    }

    private void recusarPerdido(Cenario cenario, Lead lead, String json, String mensagem) {
        perdido(lead.getId(), json).statusCode(400).body("mensagem", is(mensagem));
        assertAberto(cenario, lead);
    }

    private void assertAberto(Cenario cenario, Lead lead) {
        fresco();
        var negocio = negocios.buscar(lead.getId(), cenario.empresa()).orElseThrow();
        assertEquals(cenario.abertaId(), negocio.etapaId());
        assertNull(negocio.planoFechado());
        assertNull(negocio.valorFechado());
        assertNull(negocio.motivoPerdaId());
        assertNull(negocio.observacao());
        assertEquals(SituacaoLead.ABERTA, leads.buscarPorId(lead.getId(), cenario.empresa()).orElseThrow().situacao());
        if (cenario.comDono())
            assertEquals(cenario.dono(),
                    distribuicao.buscarDono(cenario.empresa().valor(), lead.getId()).orElseThrow().ownerId());
        else
            assertTrue(distribuicao.buscarDono(cenario.empresa().valor(), lead.getId()).isEmpty());
        assertEquals(0, contarHistorico(cenario.abertaId(), cenario.ganhoId(), cenario.perdidoId()));
        assertSemEvento();
    }

    private ValidatableResponse ganho(UUID leadId, String json) {
        return given().contentType(ContentType.JSON)
                .body(json)
                .when().post("/funil/leads/" + leadId + "/ganho")
                .then();
    }

    private ValidatableResponse perdido(UUID leadId, String json) {
        return given().contentType(ContentType.JSON)
                .body(json)
                .when().post("/funil/leads/" + leadId + "/perdido")
                .then();
    }

    private int carga(String companyId, UUID dono) {
        fresco();
        return distribuicao.cargasAbertas(companyId).getOrDefault(dono, 0);
    }

    private boolean listadoSemDono(String companyId, UUID leadId) {
        fresco();
        return fatos.listarSemDono(companyId, new FatosLead.Filtro(null, null, null, null)).stream()
                .anyMatch(fato -> leadId.equals(fato.id()));
    }

    private boolean listadoNaCarteira(String companyId, UUID dono, UUID leadId) {
        fresco();
        return fatos.listarCarteiraAberta(companyId, dono).stream()
                .anyMatch(fato -> leadId.equals(fato.id()));
    }

    private UUID unicaAtiva(CompanyId empresa, TipoEtapa tipo) {
        fresco();
        var ativas = etapas.listarPorEmpresa(empresa).stream()
                .filter(etapa -> etapa.tipo() == tipo && etapa.ativa())
                .toList();
        assertEquals(1, ativas.size());
        return ativas.getFirst().getId();
    }

    private Cenario preparar(String companyId, UUID dono, boolean comDono) {
        if (dono != null)
            criarUsuario(UUID.randomUUID(), dono);
        var aberta = etapa(companyId, TipoEtapa.ABERTO, 1);
        var ganho = etapa(companyId, TipoEtapa.GANHO, 2);
        var perdido = etapa(companyId, TipoEtapa.PERDIDO, 3);
        return new Cenario(CompanyId.reconstituir(companyId), aberta.getId(), ganho.getId(), perdido.getId(), dono,
                comDono);
    }

    private Lead colocar(Cenario cenario, String telefone, SituacaoLead situacao, UUID etapaId) {
        var lead = gravarLead(cenario.empresa(), telefone, situacao);
        var negocio = NegocioFunil.tryCreate(lead.getId(), etapaId, ENTRADA);
        assertTrue(negocio.isOk());
        negocios.salvar(negocio.getValue());
        if (cenario.comDono())
            distribuicao.salvarDono(
                    new DonoLead(lead.getId(), cenario.empresa().valor(), cenario.dono(), ENTRADA, false), ENTRADA);
        return lead;
    }

    private MotivoPerda motivo(String companyId, String nome, boolean ativo) {
        var criado = MotivoPerda.tryCreate(companyId, nome, ativo);
        assertTrue(criado.isOk());
        motivos.salvar(criado.getValue());
        return criado.getValue();
    }

    private Etapa etapa(String companyId, TipoEtapa tipo, int ordem) {
        var criada = Etapa.tryCreate(companyId, tipo.name() + "-" + ordem, tipo, ordem, true, null, null, null, false,
                false);
        assertTrue(criada.isOk());
        etapas.salvar(criada.getValue());
        return criada.getValue();
    }

    private Lead gravarLead(CompanyId empresa, String telefone, SituacaoLead situacao) {
        var agora = Instant.parse("2026-04-01T12:00:00Z");
        var lead = Lead.reconstituir(UUID.randomUUID(), agora, agora, null, empresa,
                NomeContato.reconstituir("Ana"), TelefoneBrasil.reconstituir(telefone), null, null,
                OrigemLead.IMPORTACAO, situacao, null, null, false);
        leads.salvar(lead);
        return lead;
    }

    private void assertHistorico(UUID origem, UUID destino, UUID userId, int quantidade) {
        var linhas = historicosEntre(origem, destino);
        assertEquals(quantidade, linhas.size());
        for (var linha : linhas) {
            assertEquals(origem, linha.origemEtapaId);
            assertEquals(destino, linha.destinoEtapaId);
            assertEquals(userId, linha.userId);
            assertTrue(linha.instante != null);
        }
    }

    private List<HistoricoEtapaEntity> historicosEntre(UUID origem, UUID destino) {
        fresco();
        return em.createQuery("""
                        from HistoricoEtapaEntity h
                        where h.origemEtapaId = :origem and h.destinoEtapaId = :destino
                        """, HistoricoEtapaEntity.class)
                .setParameter("origem", origem)
                .setParameter("destino", destino)
                .getResultList();
    }

    private long contarHistorico(UUID... ids) {
        fresco();
        return em.createQuery("""
                        select count(h) from HistoricoEtapaEntity h
                        where h.origemEtapaId in :ids or h.destinoEtapaId in :ids
                        """, Long.class)
                .setParameter("ids", List.of(ids))
                .getSingleResult();
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

    private void assertSemEvento() {
        assertEquals(0, captura.ocorrencias(LeadMudouEtapa.class));
        assertEquals(0, captura.ocorrencias(LeadGanho.class));
        assertEquals(0, captura.ocorrencias(LeadPerdido.class));
    }

    private void fresco() {
        QuarkusTransaction.requiringNew().run(em::clear);
    }

    private record Cenario(CompanyId empresa, UUID abertaId, UUID ganhoId, UUID perdidoId, UUID dono,
                           boolean comDono) {
    }
}
