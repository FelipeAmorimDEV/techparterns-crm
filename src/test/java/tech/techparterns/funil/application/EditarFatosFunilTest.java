package tech.techparterns.funil.application;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import tech.techparterns.distribuicao.domain.model.ValidacaoDistribuicao;
import tech.techparterns.distribuicao.domain.repository.DistribuicaoRepository;
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
class EditarFatosFunilTest {

    private static final Instant ENTRADA = Instant.parse("2026-04-01T15:30:00Z");
    private static final String SITUACAO_FINAL = "Situação final não muda";
    private static final String PLANO_UM = "P";
    private static final String PLANO_QUARENTA = "abcdefghijklmnopqrstuvwxyz0123456789ABCD";
    private static final String UF_INICIAL = "SP";
    private static final String CIDADE_INICIAL = "Campinas";

    private int telefones = 1911101101;

    @Inject
    EtapaRepository etapas;
    @Inject
    MotivoPerdaRepository motivos;
    @Inject
    NegocioFunilRepository negocios;
    @Inject
    LeadRepository leads;
    @Inject
    ExcluirLeadUseCase excluirLead;
    @Inject
    DistribuicaoRepository distribuicao;
    @Inject
    CapturaEventosFunil captura;
    @Inject
    EntityManager em;

    @Test
    @TestSecurity(user = "9d110001-9d11-49d1-89d1-000000000001", roles = "ADMIN")
    @JwtSecurity(claims = {
            @Claim(key = "sub", value = "9d110001-9d11-49d1-89d1-000000000001"),
            @Claim(key = "companyId", value = "funil-t11-ganho-invalido")
    })
    void ganhoSemParValidoNaoGravaSituacaoEtapaNemFatos() {
        assertEquals(1, PLANO_UM.length());
        var admin = UUID.fromString("9d110001-9d11-49d1-89d1-000000000001");
        var dono = UUID.fromString("9d110001-9d11-49d1-89d1-000000000011");
        var cenario = preparar("funil-t11-ganho-invalido", dono, admin);
        var lead = colocar(cenario, SituacaoLead.ABERTA, cenario.abertaId(), false, UF_INICIAL, CIDADE_INICIAL);
        var acima = "  " + "c".repeat(41) + "  ";
        assertEquals(41, acima.trim().length());

        recusarAberto(cenario, lead, pedido("GANHO", true, "RJ", "Niterói", "\"valorFechado\":10.00"),
                TextoPlano.VAZIO);
        recusarAberto(cenario, lead, pedido("GANHO", true, "RJ", "Niterói", "\"planoFechado\":\"\",\"valorFechado\":10.00"),
                TextoPlano.VAZIO);
        recusarAberto(cenario, lead, pedido("GANHO", true, "RJ", "Niterói", "\"planoFechado\":\"   \",\"valorFechado\":10.00"),
                TextoPlano.VAZIO);
        recusarAberto(cenario, lead,
                pedido("GANHO", true, "RJ", "Niterói", "\"planoFechado\":\"" + acima + "\",\"valorFechado\":10.00"),
                TextoPlano.LONGO);
        recusarAberto(cenario, lead, pedido("GANHO", true, "RJ", "Niterói", "\"planoFechado\":\"ok\""),
                MarcarGanhoUseCase.VALOR_FECHADO_NAO_POSITIVO);
        recusarAberto(cenario, lead, pedido("GANHO", true, "RJ", "Niterói", "\"planoFechado\":\"ok\",\"valorFechado\":0"),
                MarcarGanhoUseCase.VALOR_FECHADO_NAO_POSITIVO);
        recusarAberto(cenario, lead, pedido("GANHO", true, "RJ", "Niterói", "\"planoFechado\":\"ok\",\"valorFechado\":-1"),
                MarcarGanhoUseCase.VALOR_FECHADO_NAO_POSITIVO);
    }

    @Test
    @TestSecurity(user = "9d110002-9d11-49d1-89d1-000000000002", roles = "ADMIN")
    @JwtSecurity(claims = {
            @Claim(key = "sub", value = "9d110002-9d11-49d1-89d1-000000000002"),
            @Claim(key = "companyId", value = "funil-t11-ganho-admin")
    })
    void ganhoComPlanoDeUmEDeQuarentaGravaFatosDoPedido() {
        assertEquals(40, PLANO_QUARENTA.length());
        var admin = UUID.fromString("9d110002-9d11-49d1-89d1-000000000002");
        var dono = UUID.fromString("9d110002-9d11-49d1-89d1-000000000012");
        var cenario = preparar("funil-t11-ganho-admin", dono, admin);
        var curto = colocar(cenario, SituacaoLead.ABERTA, cenario.abertaId(), false, UF_INICIAL, CIDADE_INICIAL);
        var longo = colocar(cenario, SituacaoLead.ABERTA, cenario.abertaId(), true, "MG", "BH");
        captura.limpar();

        patch(curto.getId(), pedido("GANHO", true, "rj", "Niterói",
                "\"planoFechado\":\"" + PLANO_UM + "\",\"valorFechado\":1.00")).statusCode(204);
        patch(longo.getId(), pedido("GANHO", false, "ba", "Salvador",
                "\"planoFechado\":\"" + PLANO_QUARENTA + "\",\"valorFechado\":10.00")).statusCode(204);

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
        assertFatos(cenario, curto, SituacaoLead.GANHO, true, "RJ", "Niterói");
        assertFatos(cenario, longo, SituacaoLead.GANHO, false, "BA", "Salvador");
        assertEquals(dono, distribuicao.buscarDono(cenario.empresa().valor(), curto.getId()).orElseThrow().ownerId());
        assertEquals(dono, distribuicao.buscarDono(cenario.empresa().valor(), longo.getId()).orElseThrow().ownerId());
        assertHistorico(cenario.abertaId(), etapaGanho, admin, 2);
        assertEquals(2, captura.eventos().size());
        assertEquals(2, captura.ocorrencias(LeadGanho.class));
        assertEquals(0, captura.ocorrencias(LeadMudouEtapa.class));
        assertEquals(0, captura.ocorrencias(LeadPerdido.class));
    }

    @Test
    @TestSecurity(user = "9d110003-9d11-49d1-89d1-000000000003", roles = "SELLER")
    @JwtSecurity(claims = {
            @Claim(key = "sub", value = "9d110003-9d11-49d1-89d1-000000000003"),
            @Claim(key = "companyId", value = "funil-t11-ganho-dono")
    })
    void vendedorDonoGravaUfECidadeDoPedidoQuandoGanhoConclui() {
        var dono = UUID.fromString("9d110003-9d11-49d1-89d1-000000000003");
        var cenario = preparar("funil-t11-ganho-dono", dono, dono);
        var lead = colocar(cenario, SituacaoLead.ABERTA, cenario.abertaId(), false, UF_INICIAL, CIDADE_INICIAL);
        captura.limpar();

        patch(lead.getId(), pedido("GANHO", true, "rj", "Niterói",
                "\"planoFechado\":\"  plano  \",\"valorFechado\":8.00")).statusCode(204);

        fresco();
        var etapaGanho = unicaAtiva(cenario.empresa(), TipoEtapa.GANHO);
        var negocio = negocios.buscar(lead.getId(), cenario.empresa()).orElseThrow();
        assertEquals(etapaGanho, negocio.etapaId());
        assertEquals("plano", negocio.planoFechado().valor());
        assertEquals(0, new BigDecimal("8.00").compareTo(negocio.valorFechado().valor()));
        assertFatos(cenario, lead, SituacaoLead.GANHO, true, "RJ", "Niterói");
        assertEquals(dono, distribuicao.buscarDono(cenario.empresa().valor(), lead.getId()).orElseThrow().ownerId());
        assertHistorico(cenario.abertaId(), etapaGanho, dono, 1);
        assertEquals(1, captura.eventos().size());
        assertEquals(1, captura.ocorrencias(LeadGanho.class));
        assertEquals(0, captura.ocorrencias(LeadMudouEtapa.class));
        assertEquals(0, captura.ocorrencias(LeadPerdido.class));
    }

    @Test
    @TestSecurity(user = "9d110004-9d11-49d1-89d1-000000000004", roles = "SELLER")
    @JwtSecurity(claims = {
            @Claim(key = "sub", value = "9d110004-9d11-49d1-89d1-000000000004"),
            @Claim(key = "companyId", value = "funil-t11-motivo")
    })
    void perdidoSemMotivoAtivoNaoGravaSituacaoEtapaNemFatos() {
        var dono = UUID.fromString("9d110004-9d11-49d1-89d1-000000000004");
        var cenario = preparar("funil-t11-motivo", dono, dono);
        var lead = colocar(cenario, SituacaoLead.ABERTA, cenario.abertaId(), false, UF_INICIAL, CIDADE_INICIAL);
        var inativo = motivo(cenario.empresa().valor(), "Inativo", false);
        var alheio = motivo("funil-t11-motivo-alheio", "Alheio", true);

        recusarAberto(cenario, lead, pedido("PERDIDO", true, "RJ", "Niterói", null),
                MarcarPerdidoUseCase.MOTIVO_INVALIDO);
        recusarAberto(cenario, lead, pedido("PERDIDO", true, "RJ", "Niterói", "\"motivoPerdaId\":null"),
                MarcarPerdidoUseCase.MOTIVO_INVALIDO);
        recusarAberto(cenario, lead,
                pedido("PERDIDO", true, "RJ", "Niterói", "\"motivoPerdaId\":\"" + inativo.getId() + "\""),
                MarcarPerdidoUseCase.MOTIVO_INVALIDO);
        recusarAberto(cenario, lead,
                pedido("PERDIDO", true, "RJ", "Niterói", "\"motivoPerdaId\":\"" + alheio.getId() + "\""),
                MarcarPerdidoUseCase.MOTIVO_INVALIDO);
    }

    @Test
    @TestSecurity(user = "9d110005-9d11-49d1-89d1-000000000005", roles = "ADMIN")
    @JwtSecurity(claims = {
            @Claim(key = "sub", value = "9d110005-9d11-49d1-89d1-000000000005"),
            @Claim(key = "companyId", value = "funil-t11-perdido")
    })
    void perdidoComMotivoAtivoGravaFatosDoPedidoEObservacaoInteira() {
        var admin = UUID.fromString("9d110005-9d11-49d1-89d1-000000000005");
        var dono = UUID.fromString("9d110005-9d11-49d1-89d1-000000000015");
        var cenario = preparar("funil-t11-perdido", dono, admin);
        var motivo = motivo(cenario.empresa().valor(), "Preco", true);
        var semObservacao = colocar(cenario, SituacaoLead.ABERTA, cenario.abertaId(), false, UF_INICIAL,
                CIDADE_INICIAL);
        var comObservacao = colocar(cenario, SituacaoLead.ABERTA, cenario.abertaId(), true, "MG", "BH");
        var longa = "o".repeat(301);
        assertEquals(301, longa.length());
        captura.limpar();

        patch(semObservacao.getId(), pedido("PERDIDO", true, "rj", "Niterói",
                "\"motivoPerdaId\":\"" + motivo.getId() + "\"")).statusCode(204);
        patch(comObservacao.getId(), pedido("PERDIDO", false, "pr", "Curitiba",
                "\"motivoPerdaId\":\"" + motivo.getId() + "\",\"observacao\":\"" + longa + "\"")).statusCode(204);

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
        assertFatos(cenario, semObservacao, SituacaoLead.PERDIDO, true, "RJ", "Niterói");
        assertFatos(cenario, comObservacao, SituacaoLead.PERDIDO, false, "PR", "Curitiba");
        assertEquals(dono,
                distribuicao.buscarDono(cenario.empresa().valor(), semObservacao.getId()).orElseThrow().ownerId());
        assertEquals(dono,
                distribuicao.buscarDono(cenario.empresa().valor(), comObservacao.getId()).orElseThrow().ownerId());
        assertHistorico(cenario.abertaId(), etapaPerdido, admin, 2);
        assertEquals(2, captura.eventos().size());
        assertEquals(2, captura.ocorrencias(LeadPerdido.class));
        assertEquals(0, captura.ocorrencias(LeadMudouEtapa.class));
        assertEquals(0, captura.ocorrencias(LeadGanho.class));
    }

    @Test
    @TestSecurity(user = "9d110006-9d11-49d1-89d1-000000000006", roles = "ADMIN")
    @JwtSecurity(claims = {
            @Claim(key = "sub", value = "9d110006-9d11-49d1-89d1-000000000006"),
            @Claim(key = "companyId", value = "funil-t11-aberta-admin")
    })
    void abertaAdminGravaUfECidadeENaoExigePlanoNemMotivo() {
        var admin = UUID.fromString("9d110006-9d11-49d1-89d1-000000000006");
        var dono = UUID.fromString("9d110006-9d11-49d1-89d1-000000000016");
        var cenario = preparar("funil-t11-aberta-admin", dono, admin);
        var lead = colocar(cenario, SituacaoLead.ABERTA, cenario.abertaId(), false, UF_INICIAL, CIDADE_INICIAL);
        var cidadeLonga = "c".repeat(81);
        assertEquals(81, cidadeLonga.length());

        recusarAberto(cenario, lead, pedido("ABERTA", true, "S", "São Paulo", null),
                ValidacaoDistribuicao.UF_INVALIDA);
        recusarAberto(cenario, lead, pedido("ABERTA", true, "RJ", cidadeLonga, null),
                ValidacaoDistribuicao.CIDADE_INVALIDA);

        captura.limpar();
        patch(lead.getId(), pedido("ABERTA", true, "mg", "Belo Horizonte",
                "\"motivoPerdaId\":\"" + UUID.randomUUID() + "\"")).statusCode(204);

        fresco();
        assertFatos(cenario, lead, SituacaoLead.ABERTA, true, "MG", "Belo Horizonte");
        assertEquals(cenario.abertaId(), negocios.buscar(lead.getId(), cenario.empresa()).orElseThrow().etapaId());
        assertEquals(dono, distribuicao.buscarDono(cenario.empresa().valor(), lead.getId()).orElseThrow().ownerId());
        assertEquals(0, contarHistorico(cenario.abertaId(), cenario.ganhoId(), cenario.perdidoId()));
        assertSemEvento();
    }

    @Test
    @TestSecurity(user = "9d110007-9d11-49d1-89d1-000000000007", roles = "SELLER")
    @JwtSecurity(claims = {
            @Claim(key = "sub", value = "9d110007-9d11-49d1-89d1-000000000007"),
            @Claim(key = "companyId", value = "funil-t11-aberta-dono")
    })
    void abertaVendedorDonoConservaUfECidadeEGravaQuente() {
        var dono = UUID.fromString("9d110007-9d11-49d1-89d1-000000000007");
        var cenario = preparar("funil-t11-aberta-dono", dono, dono);
        var lead = colocar(cenario, SituacaoLead.ABERTA, cenario.abertaId(), false, UF_INICIAL, CIDADE_INICIAL);
        captura.limpar();

        patch(lead.getId(), pedido("ABERTA", true, "MG", "Belo Horizonte", null)).statusCode(204);
        patch(lead.getId(), pedido("ABERTA", true, "RJ", "Niterói",
                "\"planoFechado\":\"qualquer\",\"motivoPerdaId\":\"" + UUID.randomUUID() + "\"")).statusCode(204);

        fresco();
        assertFatos(cenario, lead, SituacaoLead.ABERTA, true, UF_INICIAL, CIDADE_INICIAL);
        assertEquals(cenario.abertaId(), negocios.buscar(lead.getId(), cenario.empresa()).orElseThrow().etapaId());
        assertEquals(dono, distribuicao.buscarDono(cenario.empresa().valor(), lead.getId()).orElseThrow().ownerId());
        assertEquals(0, contarHistorico(cenario.abertaId(), cenario.ganhoId(), cenario.perdidoId()));
        assertSemEvento();
    }

    @Test
    @TestSecurity(user = "9d110008-9d11-49d1-89d1-000000000008", roles = "ADMIN")
    @JwtSecurity(claims = {
            @Claim(key = "sub", value = "9d110008-9d11-49d1-89d1-000000000008"),
            @Claim(key = "companyId", value = "funil-t11-final")
    })
    void leadGanhoOuPerdidoNaoReabreNemGravaOPedido() {
        var admin = UUID.fromString("9d110008-9d11-49d1-89d1-000000000008");
        var dono = UUID.fromString("9d110008-9d11-49d1-89d1-000000000018");
        var cenario = preparar("funil-t11-final", dono, admin);
        var motivo = motivo(cenario.empresa().valor(), "Preco", true);
        var ganho = colocar(cenario, SituacaoLead.GANHO, cenario.ganhoId(), true, UF_INICIAL, CIDADE_INICIAL);
        var perdido = colocar(cenario, SituacaoLead.PERDIDO, cenario.perdidoId(), false, "RJ", "Niterói");
        var corpoAberto = pedido("ABERTA", false, "MG", "BH", null);
        var corpoGanho = pedido("GANHO", false, "MG", "BH", "\"planoFechado\":\"Plano\",\"valorFechado\":10.00");
        var corpoPerdido = pedido("PERDIDO", true, "MG", "BH", "\"motivoPerdaId\":\"" + motivo.getId() + "\"");

        recusar(ganho.getId(), corpoAberto, SITUACAO_FINAL);
        recusar(ganho.getId(), corpoPerdido, SITUACAO_FINAL);
        recusar(ganho.getId(), corpoGanho, SITUACAO_FINAL);
        recusar(perdido.getId(), corpoAberto, SITUACAO_FINAL);
        recusar(perdido.getId(), corpoGanho, SITUACAO_FINAL);
        recusar(perdido.getId(), corpoPerdido, SITUACAO_FINAL);

        fresco();
        assertFatos(cenario, ganho, SituacaoLead.GANHO, true, UF_INICIAL, CIDADE_INICIAL);
        assertFatos(cenario, perdido, SituacaoLead.PERDIDO, false, "RJ", "Niterói");
        assertEquals(cenario.ganhoId(), negocios.buscar(ganho.getId(), cenario.empresa()).orElseThrow().etapaId());
        assertEquals(cenario.perdidoId(), negocios.buscar(perdido.getId(), cenario.empresa()).orElseThrow().etapaId());
        assertNull(negocios.buscar(ganho.getId(), cenario.empresa()).orElseThrow().planoFechado());
        assertNull(negocios.buscar(perdido.getId(), cenario.empresa()).orElseThrow().motivoPerdaId());
        assertEquals(dono, distribuicao.buscarDono(cenario.empresa().valor(), ganho.getId()).orElseThrow().ownerId());
        assertEquals(dono, distribuicao.buscarDono(cenario.empresa().valor(), perdido.getId()).orElseThrow().ownerId());
        assertEquals(0, contarHistorico(cenario.abertaId(), cenario.ganhoId(), cenario.perdidoId()));
        assertSemEvento();
    }

    @Test
    @TestSecurity(user = "9d110009-9d11-49d1-89d1-000000000009", roles = "SELLER")
    @JwtSecurity(claims = {
            @Claim(key = "sub", value = "9d110009-9d11-49d1-89d1-000000000009"),
            @Claim(key = "companyId", value = "funil-t11-nao-dono")
    })
    void vendedorQueNaoEDonoNaoGravaInclusiveGanhoEPerdido() {
        var alheio = UUID.fromString("9d110009-9d11-49d1-89d1-000000000009");
        var dono = UUID.fromString("9d110009-9d11-49d1-89d1-000000000019");
        var cenario = preparar("funil-t11-nao-dono", dono, alheio);
        var lead = colocar(cenario, SituacaoLead.ABERTA, cenario.abertaId(), false, UF_INICIAL, CIDADE_INICIAL);
        var motivo = motivo(cenario.empresa().valor(), "Preco", true);

        recusarAberto(cenario, lead, pedido("GANHO", true, "RJ", "Niterói",
                "\"planoFechado\":\"Plano\",\"valorFechado\":10.00"), ValidacaoDistribuicao.NAO_E_DONO);
        recusarAberto(cenario, lead, pedido("PERDIDO", true, "RJ", "Niterói",
                "\"motivoPerdaId\":\"" + motivo.getId() + "\""), ValidacaoDistribuicao.NAO_E_DONO);
        recusarAberto(cenario, lead, pedido("ABERTA", true, "RJ", "Niterói", null),
                ValidacaoDistribuicao.NAO_E_DONO);
    }

    @Test
    @TestSecurity(user = "9d11000a-9d11-49d1-89d1-00000000000a", roles = "ADMIN")
    @JwtSecurity(claims = {
            @Claim(key = "sub", value = "9d11000a-9d11-49d1-89d1-00000000000a"),
            @Claim(key = "companyId", value = "funil-t11-uf-admin")
    })
    void ufOuCidadeInvalidaRecusaGanhoEPerdidoDoAdmin() {
        var admin = UUID.fromString("9d11000a-9d11-49d1-89d1-00000000000a");
        var dono = UUID.fromString("9d11000a-9d11-49d1-89d1-00000000001a");
        var cenario = preparar("funil-t11-uf-admin", dono, admin);
        var lead = colocar(cenario, SituacaoLead.ABERTA, cenario.abertaId(), false, UF_INICIAL, CIDADE_INICIAL);
        var motivo = motivo(cenario.empresa().valor(), "Preco", true);
        var cidadeLonga = "c".repeat(81);
        var ganho = "\"planoFechado\":\"Plano\",\"valorFechado\":10.00";
        var perdido = "\"motivoPerdaId\":\"" + motivo.getId() + "\"";

        recusarAberto(cenario, lead, pedido("GANHO", true, "S", "Niterói", ganho),
                ValidacaoDistribuicao.UF_INVALIDA);
        recusarAberto(cenario, lead, pedido("GANHO", true, "RJ", cidadeLonga, ganho),
                ValidacaoDistribuicao.CIDADE_INVALIDA);
        recusarAberto(cenario, lead, pedido("PERDIDO", true, "S", "Niterói", perdido),
                ValidacaoDistribuicao.UF_INVALIDA);
        recusarAberto(cenario, lead, pedido("PERDIDO", true, "RJ", cidadeLonga, perdido),
                ValidacaoDistribuicao.CIDADE_INVALIDA);
    }

    @Test
    @TestSecurity(user = "9d11000b-9d11-49d1-89d1-00000000000b", roles = "SELLER")
    @JwtSecurity(claims = {
            @Claim(key = "sub", value = "9d11000b-9d11-49d1-89d1-00000000000b"),
            @Claim(key = "companyId", value = "funil-t11-uf-dono")
    })
    void ufOuCidadeInvalidaRecusaGanhoEPerdidoDoVendedorDono() {
        var dono = UUID.fromString("9d11000b-9d11-49d1-89d1-00000000000b");
        var cenario = preparar("funil-t11-uf-dono", dono, dono);
        var lead = colocar(cenario, SituacaoLead.ABERTA, cenario.abertaId(), false, UF_INICIAL, CIDADE_INICIAL);
        var motivo = motivo(cenario.empresa().valor(), "Preco", true);
        var cidadeLonga = "c".repeat(81);
        var ganho = "\"planoFechado\":\"Plano\",\"valorFechado\":10.00";
        var perdido = "\"motivoPerdaId\":\"" + motivo.getId() + "\"";

        recusarAberto(cenario, lead, pedido("GANHO", true, "S", "Niterói", ganho),
                ValidacaoDistribuicao.UF_INVALIDA);
        recusarAberto(cenario, lead, pedido("GANHO", true, "RJ", cidadeLonga, ganho),
                ValidacaoDistribuicao.CIDADE_INVALIDA);
        recusarAberto(cenario, lead, pedido("PERDIDO", true, "S", "Niterói", perdido),
                ValidacaoDistribuicao.UF_INVALIDA);
        recusarAberto(cenario, lead, pedido("PERDIDO", true, "RJ", cidadeLonga, perdido),
                ValidacaoDistribuicao.CIDADE_INVALIDA);
    }

    @Test
    @TestSecurity(user = "9d11000c-9d11-49d1-89d1-00000000000c", roles = "ADMIN")
    @JwtSecurity(claims = {
            @Claim(key = "sub", value = "9d11000c-9d11-49d1-89d1-00000000000c"),
            @Claim(key = "companyId", value = "funil-t11-excluir")
    })
    void leadExcluidoNaoConcluiGanhoNemPerdido() {
        var admin = UUID.fromString("9d11000c-9d11-49d1-89d1-00000000000c");
        var cenario = preparar("funil-t11-excluir", admin, admin);
        var lead = colocar(cenario, SituacaoLead.ABERTA, cenario.abertaId(), false, UF_INICIAL, CIDADE_INICIAL);
        var motivo = motivo(cenario.empresa().valor(), "Preco", true);
        excluirLead.execute(new ExcluirLeadCommand(lead.getId()));
        captura.limpar();

        recusarAberto(cenario, lead, pedido("GANHO", true, "RJ", "Niterói",
                "\"planoFechado\":\"Plano\",\"valorFechado\":10.00"), MoverLeadEtapaUseCase.LEAD_NAO_ENCONTRADO);
        recusarAberto(cenario, lead, pedido("PERDIDO", true, "RJ", "Niterói",
                "\"motivoPerdaId\":\"" + motivo.getId() + "\""), MoverLeadEtapaUseCase.LEAD_NAO_ENCONTRADO);

        fresco();
        var gravado = leads.buscarPorId(lead.getId(), cenario.empresa()).orElseThrow();
        assertEquals(SituacaoLead.ABERTA, gravado.situacao());
        assertTrue(gravado.isDeleted());
    }

    private void recusarAberto(Cenario cenario, Lead lead, String json, String mensagem) {
        recusar(lead.getId(), json, mensagem);
        assertAberto(cenario, lead);
    }

    private void recusar(UUID leadId, String json, String mensagem) {
        captura.limpar();
        patch(leadId, json).statusCode(400).body("mensagem", is(mensagem));
        assertSemEvento();
    }

    private void assertAberto(Cenario cenario, Lead lead) {
        fresco();
        assertFatos(cenario, lead, SituacaoLead.ABERTA, false, UF_INICIAL, CIDADE_INICIAL);
        var negocio = negocios.buscar(lead.getId(), cenario.empresa()).orElseThrow();
        assertEquals(cenario.abertaId(), negocio.etapaId());
        assertNull(negocio.planoFechado());
        assertNull(negocio.valorFechado());
        assertNull(negocio.motivoPerdaId());
        assertNull(negocio.observacao());
        assertEquals(cenario.dono(),
                distribuicao.buscarDono(cenario.empresa().valor(), lead.getId()).orElseThrow().ownerId());
        assertEquals(0, contarHistorico(cenario.abertaId(), cenario.ganhoId(), cenario.perdidoId()));
        assertSemEvento();
    }

    private void assertFatos(Cenario cenario, Lead lead, SituacaoLead situacao, boolean quente, String uf,
                             String cidade) {
        fresco();
        var atual = leads.buscarPorId(lead.getId(), cenario.empresa()).orElseThrow();
        assertEquals(situacao, atual.situacao());
        assertEquals(quente, atual.quente());
        assertEquals(uf, atual.uf());
        assertEquals(cidade, atual.cidade());
    }

    private ValidatableResponse patch(UUID leadId, String json) {
        return given().contentType(ContentType.JSON)
                .body(json)
                .when().patch("/leads/" + leadId)
                .then();
    }

    private static String pedido(String situacao, boolean quente, String uf, String cidade, String cauda) {
        var json = "{\"situacao\":\"" + situacao + "\",\"quente\":" + quente
                + ",\"uf\":" + jsonTexto(uf) + ",\"cidade\":" + jsonTexto(cidade);
        if (cauda == null || cauda.isBlank())
            return json + "}";
        return json + "," + cauda + "}";
    }

    private static String jsonTexto(String valor) {
        return valor == null ? "null" : "\"" + valor + "\"";
    }

    private UUID unicaAtiva(CompanyId empresa, TipoEtapa tipo) {
        fresco();
        var ativas = etapas.listarPorEmpresa(empresa).stream()
                .filter(etapa -> etapa.tipo() == tipo && etapa.ativa())
                .toList();
        assertEquals(1, ativas.size());
        return ativas.getFirst().getId();
    }

    private Cenario preparar(String companyId, UUID dono, UUID ator) {
        criarUsuario(dono);
        if (!dono.equals(ator))
            criarUsuario(ator);
        var aberta = etapa(companyId, TipoEtapa.ABERTO, 1);
        var ganho = etapa(companyId, TipoEtapa.GANHO, 2);
        var perdido = etapa(companyId, TipoEtapa.PERDIDO, 3);
        return new Cenario(CompanyId.reconstituir(companyId), aberta.getId(), ganho.getId(), perdido.getId(), dono);
    }

    private Lead colocar(Cenario cenario, SituacaoLead situacao, UUID etapaId, boolean quente, String uf,
                         String cidade) {
        var lead = gravarLead(cenario.empresa(), telefone(), situacao, quente, uf, cidade);
        var negocio = NegocioFunil.tryCreate(lead.getId(), etapaId, ENTRADA);
        assertTrue(negocio.isOk());
        negocios.salvar(negocio.getValue());
        distribuicao.salvarDono(
                new DonoLead(lead.getId(), cenario.empresa().valor(), cenario.dono(), ENTRADA, false), ENTRADA);
        return lead;
    }

    private String telefone() {
        return "+5511" + (telefones++);
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

    private Lead gravarLead(CompanyId empresa, String telefone, SituacaoLead situacao, boolean quente, String uf,
                            String cidade) {
        var agora = Instant.parse("2026-04-01T12:00:00Z");
        var lead = Lead.reconstituir(UUID.randomUUID(), agora, agora, null, empresa,
                NomeContato.reconstituir("Ana"), TelefoneBrasil.reconstituir(telefone), null, null,
                OrigemLead.IMPORTACAO, situacao, uf, cidade, quente);
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

    private void criarUsuario(UUID usuarioId) {
        QuarkusTransaction.requiringNew().run(() -> {
            var empresa = new EmpresaEntity();
            empresa.id = UUID.randomUUID();
            empresa.nome = "Funil";
            empresa.status = "ACTIVE";
            empresa.createdAt = Instant.parse("2026-01-01T00:00:00Z");
            empresa.updatedAt = empresa.createdAt;
            em.persist(empresa);
            var usuario = new UsuarioEmpresaEntity();
            usuario.id = usuarioId;
            usuario.empresaId = empresa.id;
            usuario.nome = "Vendedor";
            usuario.email = "funil-t11-" + usuarioId + "@funil.test";
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

    private record Cenario(CompanyId empresa, UUID abertaId, UUID ganhoId, UUID perdidoId, UUID dono) {
    }
}
