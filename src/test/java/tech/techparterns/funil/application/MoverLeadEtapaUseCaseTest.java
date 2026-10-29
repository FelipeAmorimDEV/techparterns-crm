package tech.techparterns.funil.application;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
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
import tech.techparterns.distribuicao.domain.model.DonoLead;
import tech.techparterns.distribuicao.domain.repository.DistribuicaoRepository;
import tech.techparterns.funil.application.usecase.MoverLeadEtapaUseCase;
import tech.techparterns.funil.domain.model.Etapa;
import tech.techparterns.funil.domain.model.LeadGanho;
import tech.techparterns.funil.domain.model.LeadMudouEtapa;
import tech.techparterns.funil.domain.model.LeadPerdido;
import tech.techparterns.funil.domain.model.NegocioFunil;
import tech.techparterns.funil.domain.model.TipoEtapa;
import tech.techparterns.funil.domain.repository.EtapaRepository;
import tech.techparterns.funil.domain.repository.NegocioFunilRepository;
import tech.techparterns.funil.domain.service.AtrasoEtapa;
import tech.techparterns.funil.domain.service.EntradaEtapa;
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
class MoverLeadEtapaUseCaseTest {

    private static final Instant ENTRADA = Instant.parse("2026-04-01T15:30:00Z");

    @Inject
    EtapaRepository etapas;
    @Inject
    NegocioFunilRepository negocios;
    @Inject
    LeadRepository leads;
    @Inject
    DistribuicaoRepository distribuicao;
    @Inject
    ExcluirLeadUseCase excluirLead;
    @Inject
    CapturaEventosFunil captura;
    @Inject
    Clock clock;
    @Inject
    EntityManager em;

    @Test
    @TestSecurity(user = "9a000001-9a00-49a0-89a0-000000000001", roles = "SELLER")
    @JwtSecurity(claims = {
            @Claim(key = "sub", value = "9a000001-9a00-49a0-89a0-000000000001"),
            @Claim(key = "companyId", value = "funil-t9-ida-volta")
    })
    void donoAvancaEVoltaSemMudarSituacaoNemDono() {
        var dono = UUID.fromString("9a000001-9a00-49a0-89a0-000000000001");
        var montado = montar("funil-t9-ida-volta", dono, "+5511910009001", true);
        var destino = etapa(montado.empresa().valor(), "Proposta", 2, false, false);
        captura.limpar();

        mover(montado.leadId(), destino.getId()).statusCode(204);

        fresco();
        var ida = negocios.buscar(montado.leadId(), montado.empresa()).orElseThrow();
        assertEquals(destino.getId(), ida.etapaId());
        assertEquals(SituacaoLead.ABERTA, leads.buscarPorId(montado.leadId(), montado.empresa()).orElseThrow().situacao());
        assertEquals(dono, distribuicao.buscarDono(montado.empresa().valor(), montado.leadId()).orElseThrow().ownerId());
        var linha = historico(montado.etapaId(), destino.getId());
        assertEquals(montado.etapaId(), linha.origemEtapaId);
        assertEquals(destino.getId(), linha.destinoEtapaId);
        assertEquals(dono, linha.userId);
        assertTrue(linha.instante != null);
        assertEquals(1, captura.ocorrencias(LeadMudouEtapa.class));

        mover(montado.leadId(), montado.etapaId()).statusCode(204);
        fresco();
        assertEquals(montado.etapaId(), negocios.buscar(montado.leadId(), montado.empresa()).orElseThrow().etapaId());
        assertEquals(SituacaoLead.ABERTA, leads.buscarPorId(montado.leadId(), montado.empresa()).orElseThrow().situacao());
        assertEquals(dono, distribuicao.buscarDono(montado.empresa().valor(), montado.leadId()).orElseThrow().ownerId());
        var volta = historico(destino.getId(), montado.etapaId());
        assertEquals(destino.getId(), volta.origemEtapaId);
        assertEquals(montado.etapaId(), volta.destinoEtapaId);
        assertEquals(dono, volta.userId);
        assertTrue(volta.instante != null);
        assertEquals(2, captura.ocorrencias(LeadMudouEtapa.class));
    }

    @Test
    @TestSecurity(user = "9a000002-9a00-49a0-89a0-000000000002", roles = "SELLER")
    @JwtSecurity(claims = {
            @Claim(key = "sub", value = "9a000002-9a00-49a0-89a0-000000000002"),
            @Claim(key = "companyId", value = "funil-t9-roteiro")
    })
    void roteiroIncompletoRecusaECompletoEntra() {
        var dono = UUID.fromString("9a000002-9a00-49a0-89a0-000000000002");
        var montado = montar("funil-t9-roteiro", dono, "+5511910009002", true);
        gravarRoteiro(montado, true, false, null, null, null, null);
        var destino = etapa(montado.empresa().valor(), "Qualifica", 2, true, false);
        captura.limpar();

        mover(montado.leadId(), destino.getId())
                .statusCode(400)
                .body("mensagem", is(EntradaEtapa.QUALIFICACAO_INCOMPLETA));

        fresco();
        var parado = negocios.buscar(montado.leadId(), montado.empresa()).orElseThrow();
        assertEquals(montado.etapaId(), parado.etapaId());
        assertEquals(true, parado.decisor());
        assertEquals(false, parado.dorPrincipal());
        assertNull(parado.usuariosPrevistos());
        assertNull(parado.prazoDecisao());
        assertNull(parado.orcamento());
        assertEquals(0, contarHistorico(montado.etapaId(), destino.getId()));
        assertSemEvento();

        gravarRoteiro(montado, true, false, true, false, true, null);
        mover(montado.leadId(), destino.getId()).statusCode(204);
        fresco();
        assertEquals(destino.getId(), negocios.buscar(montado.leadId(), montado.empresa()).orElseThrow().etapaId());
    }

    @Test
    @TestSecurity(user = "9a000003-9a00-49a0-89a0-000000000003", roles = "SELLER")
    @JwtSecurity(claims = {
            @Claim(key = "sub", value = "9a000003-9a00-49a0-89a0-000000000003"),
            @Claim(key = "companyId", value = "funil-t9-renome")
    })
    void renomearNaoRemoveFlag() {
        var dono = UUID.fromString("9a000003-9a00-49a0-89a0-000000000003");
        var montado = montar("funil-t9-renome", dono, "+5511910009003", true);
        var destino = etapa(montado.empresa().valor(), "Qualifica", 2, true, false);
        assertTrue(destino.alterar("Qualificação nova", 2, null, null, null).isOk());
        etapas.salvar(destino);
        fresco();
        var renomeada = etapas.buscarPorId(destino.getId(), montado.empresa()).orElseThrow();
        assertEquals("Qualificação nova", renomeada.nome());
        assertTrue(renomeada.exigeQualificacao());
        captura.limpar();

        mover(montado.leadId(), destino.getId())
                .statusCode(400)
                .body("mensagem", is(EntradaEtapa.QUALIFICACAO_INCOMPLETA));

        fresco();
        assertEquals(montado.etapaId(), negocios.buscar(montado.leadId(), montado.empresa()).orElseThrow().etapaId());
        assertTrue(etapas.buscarPorId(destino.getId(), montado.empresa()).orElseThrow().exigeQualificacao());
        assertEquals(0, contarHistorico(montado.etapaId(), destino.getId()));
        assertSemEvento();
    }

    @Test
    @TestSecurity(user = "9a000004-9a00-49a0-89a0-000000000004", roles = "SELLER")
    @JwtSecurity(claims = {
            @Claim(key = "sub", value = "9a000004-9a00-49a0-89a0-000000000004"),
            @Claim(key = "companyId", value = "funil-t9-demo")
    })
    void demoObrigatoriaRecusaEAceitaComData() {
        var dono = UUID.fromString("9a000004-9a00-49a0-89a0-000000000004");
        var montado = montar("funil-t9-demo", dono, "+5511910009004", true);
        var destino = etapa(montado.empresa().valor(), "Demo", 2, false, true);
        captura.limpar();

        mover(montado.leadId(), destino.getId())
                .statusCode(400)
                .body("mensagem", is(EntradaEtapa.DEMO_OBRIGATORIA));

        fresco();
        var parado = negocios.buscar(montado.leadId(), montado.empresa()).orElseThrow();
        assertEquals(montado.etapaId(), parado.etapaId());
        assertNull(parado.dataDemo());
        assertEquals(0, contarHistorico(montado.etapaId(), destino.getId()));
        assertSemEvento();

        var demo = LocalDate.of(2026, 6, 1);
        gravarRoteiro(montado, null, null, null, null, null, demo);
        mover(montado.leadId(), destino.getId()).statusCode(204);
        fresco();
        var entrou = negocios.buscar(montado.leadId(), montado.empresa()).orElseThrow();
        assertEquals(destino.getId(), entrou.etapaId());
        assertEquals(demo, entrou.dataDemo());
    }

    @Test
    @TestSecurity(user = "9a000005-9a00-49a0-89a0-000000000005", roles = "SELLER")
    @JwtSecurity(claims = {
            @Claim(key = "sub", value = "9a000005-9a00-49a0-89a0-000000000005"),
            @Claim(key = "companyId", value = "funil-t9-nao-dono")
    })
    void vendedorQueNaoEDonoNaoMove() {
        var dono = UUID.fromString("9a000005-9a00-49a0-89a0-000000000050");
        var montado = montar("funil-t9-nao-dono", dono, "+5511910009005", true);
        var destino = etapa(montado.empresa().valor(), "Proposta", 2, false, false);
        captura.limpar();

        mover(montado.leadId(), destino.getId())
                .statusCode(400)
                .body("mensagem", is(MoverLeadEtapaUseCase.NAO_E_DONO));

        fresco();
        assertEquals(montado.etapaId(), negocios.buscar(montado.leadId(), montado.empresa()).orElseThrow().etapaId());
        assertEquals(0, contarHistorico(montado.etapaId(), destino.getId()));
        assertSemEvento();
    }

    @Test
    @TestSecurity(user = "9a000006-9a00-49a0-89a0-000000000006", roles = "SELLER")
    @JwtSecurity(claims = {
            @Claim(key = "sub", value = "9a000006-9a00-49a0-89a0-000000000006"),
            @Claim(key = "companyId", value = "funil-t9-sem-dono")
    })
    void leadSemDonoNaoEMovidoPorVendedor() {
        var montado = montar("funil-t9-sem-dono", null, "+5511910009006", false);
        var destino = etapa(montado.empresa().valor(), "Proposta", 2, false, false);
        captura.limpar();

        mover(montado.leadId(), destino.getId())
                .statusCode(400)
                .body("mensagem", is(MoverLeadEtapaUseCase.NAO_E_DONO));

        fresco();
        assertEquals(montado.etapaId(), negocios.buscar(montado.leadId(), montado.empresa()).orElseThrow().etapaId());
        assertEquals(0, contarHistorico(montado.etapaId(), destino.getId()));
        assertSemEvento();
    }

    @Test
    @TestSecurity(user = "9a000007-9a00-49a0-89a0-000000000007", roles = "SELLER")
    @JwtSecurity(claims = {
            @Claim(key = "sub", value = "9a000007-9a00-49a0-89a0-000000000007"),
            @Claim(key = "companyId", value = "funil-t9-outra")
    })
    void leadDeOutraEmpresaNaoMove() {
        var dono = UUID.fromString("9a000007-9a00-49a0-89a0-000000000007");
        var montado = montar("funil-t9-empresa-real", dono, "+5511910009007", true);
        var destino = etapa("funil-t9-outra", "Proposta", 2, false, false);
        captura.limpar();

        mover(montado.leadId(), destino.getId())
                .statusCode(400)
                .body("mensagem", is(MoverLeadEtapaUseCase.LEAD_NAO_ENCONTRADO));

        fresco();
        assertEquals(montado.etapaId(), negocios.buscar(montado.leadId(), montado.empresa()).orElseThrow().etapaId());
        assertEquals(0, contarHistorico(montado.etapaId(), destino.getId()));
        assertSemEvento();
    }

    @Test
    @TestSecurity(user = "9a000008-9a00-49a0-89a0-000000000008", roles = "SELLER")
    @JwtSecurity(claims = {
            @Claim(key = "sub", value = "9a000008-9a00-49a0-89a0-000000000008"),
            @Claim(key = "companyId", value = "funil-t9-final")
    })
    void leadGanhoOuPerdidoNaoMove() {
        var dono = UUID.fromString("9a000008-9a00-49a0-89a0-000000000008");
        var ganho = montarSituacao("funil-t9-final", dono, "+5511910009008", SituacaoLead.GANHO);
        var destino = etapa("funil-t9-final", "Proposta", 2, false, false);
        captura.limpar();

        mover(ganho.leadId(), destino.getId())
                .statusCode(400)
                .body("mensagem", is(MoverLeadEtapaUseCase.SITUACAO_FINAL));

        var perdido = leadFinalSemNovoUsuario("funil-t9-final", dono, "+5511910009009", SituacaoLead.PERDIDO);
        mover(perdido.leadId(), destino.getId())
                .statusCode(400)
                .body("mensagem", is(MoverLeadEtapaUseCase.SITUACAO_FINAL));

        fresco();
        assertEquals(ganho.etapaId(), negocios.buscar(ganho.leadId(), ganho.empresa()).orElseThrow().etapaId());
        assertEquals(SituacaoLead.GANHO, leads.buscarPorId(ganho.leadId(), ganho.empresa()).orElseThrow().situacao());
        assertEquals(perdido.etapaId(), negocios.buscar(perdido.leadId(), perdido.empresa()).orElseThrow().etapaId());
        assertEquals(SituacaoLead.PERDIDO, leads.buscarPorId(perdido.leadId(), perdido.empresa()).orElseThrow().situacao());
        assertEquals(0, contarHistorico(ganho.etapaId(), perdido.etapaId(), destino.getId()));
        assertSemEvento();
    }

    @Test
    @TestSecurity(user = "9a000009-9a00-49a0-89a0-000000000009", roles = "SELLER")
    @JwtSecurity(claims = {
            @Claim(key = "sub", value = "9a000009-9a00-49a0-89a0-000000000009"),
            @Claim(key = "companyId", value = "funil-t9-destino")
    })
    void destinoInativoOuFinalRecusa() {
        var dono = UUID.fromString("9a000009-9a00-49a0-89a0-000000000009");
        var montado = montar("funil-t9-destino", dono, "+5511910009010", true);
        var inativa = etapa(montado.empresa().valor(), "Parada", 2, false, false);
        inativa.desativar();
        etapas.salvar(inativa);
        var ganho = etapa(montado.empresa().valor(), "Ganho", 3, false, false, TipoEtapa.GANHO);
        captura.limpar();

        mover(montado.leadId(), inativa.getId())
                .statusCode(400)
                .body("mensagem", is(MoverLeadEtapaUseCase.DESTINO_INVALIDO));
        mover(montado.leadId(), ganho.getId())
                .statusCode(400)
                .body("mensagem", is(MoverLeadEtapaUseCase.DESTINO_INVALIDO));

        fresco();
        assertEquals(montado.etapaId(), negocios.buscar(montado.leadId(), montado.empresa()).orElseThrow().etapaId());
        assertEquals(0, contarHistorico(montado.etapaId(), inativa.getId(), ganho.getId()));
        assertSemEvento();
    }

    @Test
    @TestSecurity(user = "9a00000a-9a00-49a0-89a0-00000000000a", roles = "SELLER")
    @JwtSecurity(claims = {
            @Claim(key = "sub", value = "9a00000a-9a00-49a0-89a0-00000000000a"),
            @Claim(key = "companyId", value = "funil-t9-mesma")
    })
    void mesmaEtapaNaoGravaHistoricoNemEvento() {
        var dono = UUID.fromString("9a00000a-9a00-49a0-89a0-00000000000a");
        var montado = montar("funil-t9-mesma", dono, "+5511910009011", true);
        captura.limpar();

        mover(montado.leadId(), montado.etapaId()).statusCode(204);

        assertEquals(0, contarHistorico(montado.etapaId()));
        assertSemEvento();
        fresco();
        assertEquals(ENTRADA.toEpochMilli(),
                negocios.buscar(montado.leadId(), montado.empresa()).orElseThrow().entrouNaEtapa().toEpochMilli());
    }

    @Test
    @TestSecurity(user = "9a00000b-9a00-49a0-89a0-00000000000b", roles = "ADMIN")
    @JwtSecurity(claims = {
            @Claim(key = "sub", value = "9a00000b-9a00-49a0-89a0-00000000000b"),
            @Claim(key = "companyId", value = "funil-t9-excluir")
    })
    void leadExcluidoDepoisDoMovimentoNaoGanhaHistorico() {
        var admin = UUID.fromString("9a00000b-9a00-49a0-89a0-00000000000b");
        var montado = montar("funil-t9-excluir", admin, "+5511910009012", true);
        var destino = etapa(montado.empresa().valor(), "Proposta", 2, false, false);
        mover(montado.leadId(), destino.getId()).statusCode(204);
        var antes = contarHistorico(montado.etapaId(), destino.getId());
        assertEquals(1, antes);
        var gravada = historico(montado.etapaId(), destino.getId());
        var instante = gravada.instante;
        assertEquals(montado.etapaId(), gravada.origemEtapaId);
        assertEquals(destino.getId(), gravada.destinoEtapaId);
        assertEquals(admin, gravada.userId);
        assertTrue(instante != null);

        captura.limpar();
        excluirLead.execute(new ExcluirLeadCommand(montado.leadId()));
        mover(montado.leadId(), montado.etapaId())
                .statusCode(400)
                .body("mensagem", is(MoverLeadEtapaUseCase.LEAD_NAO_ENCONTRADO));

        assertEquals(antes, contarHistorico(montado.etapaId(), destino.getId()));
        var ainda = historico(montado.etapaId(), destino.getId());
        assertEquals(montado.etapaId(), ainda.origemEtapaId);
        assertEquals(destino.getId(), ainda.destinoEtapaId);
        assertEquals(admin, ainda.userId);
        assertEquals(instante, ainda.instante);
        assertSemEvento();
    }

    @Test
    @TestSecurity(user = "9a00000c-9a00-49a0-89a0-00000000000c", roles = "SELLER")
    @JwtSecurity(claims = {
            @Claim(key = "sub", value = "9a00000c-9a00-49a0-89a0-00000000000c"),
            @Claim(key = "companyId", value = "funil-t9-atraso")
    })
    void cardAtrasadoAindaMudaDeEtapa() {
        var dono = UUID.fromString("9a00000c-9a00-49a0-89a0-00000000000c");
        var montado = montar("funil-t9-atraso", dono, "+5511910009013", true);
        captura.limpar();
        var origem = etapas.buscarPorId(montado.etapaId(), montado.empresa()).orElseThrow();
        assertTrue(origem.alterar(origem.nome(), origem.ordem().valor(), 0, null, 1).isOk());
        etapas.salvar(origem);
        fresco();
        var origemLida = etapas.buscarPorId(montado.etapaId(), montado.empresa()).orElseThrow();
        var antes = negocios.buscar(montado.leadId(), montado.empresa()).orElseThrow();
        assertEquals(ENTRADA.toEpochMilli(), antes.entrouNaEtapa().toEpochMilli());
        assertEquals(Integer.valueOf(1), origemLida.sla().valor());
        assertTrue(AtrasoEtapa.atrasado(antes.entrouNaEtapa(), clock.instant(), origemLida.sla()));
        assertSemEvento();

        var destino = etapa(montado.empresa().valor(), "Proposta", 2, false, false);
        mover(montado.leadId(), destino.getId()).statusCode(204);
        fresco();
        assertEquals(destino.getId(), negocios.buscar(montado.leadId(), montado.empresa()).orElseThrow().etapaId());
        assertEquals(1, captura.eventos().size());
        assertEquals(1, captura.ocorrencias(LeadMudouEtapa.class));
        assertEquals(0, captura.ocorrencias(LeadGanho.class));
        assertEquals(0, captura.ocorrencias(LeadPerdido.class));
    }

    private io.restassured.response.ValidatableResponse mover(UUID leadId, UUID etapaId) {
        return given().contentType(ContentType.JSON)
                .body("{\"etapaId\":\"" + etapaId + "\"}")
                .when().post("/funil/leads/" + leadId + "/mover")
                .then();
    }

    private Montado montar(String companyId, UUID dono, String telefone, boolean comDono) {
        if (dono != null)
            criarUsuario(UUID.randomUUID(), dono);
        var empresa = CompanyId.reconstituir(companyId);
        var lead = gravarLead(empresa, telefone, SituacaoLead.ABERTA, null);
        var etapa = etapa(companyId, "Aberto", 1, false, false);
        var negocio = NegocioFunil.tryCreate(lead.getId(), etapa.getId(), ENTRADA);
        assertTrue(negocio.isOk());
        negocios.salvar(negocio.getValue());
        if (comDono)
            distribuicao.salvarDono(new DonoLead(lead.getId(), companyId, dono, ENTRADA, false), ENTRADA);
        return new Montado(empresa, lead, etapa.getId());
    }

    private Montado leadFinalSemNovoUsuario(String companyId, UUID dono, String telefone, SituacaoLead situacao) {
        var empresa = CompanyId.reconstituir(companyId);
        var lead = gravarLead(empresa, telefone, situacao, null);
        var etapa = etapa(companyId, "Aberto " + situacao, 6, false, false);
        var negocio = NegocioFunil.tryCreate(lead.getId(), etapa.getId(), ENTRADA);
        assertTrue(negocio.isOk());
        negocios.salvar(negocio.getValue());
        distribuicao.salvarDono(new DonoLead(lead.getId(), companyId, dono, ENTRADA, false), ENTRADA);
        return new Montado(empresa, lead, etapa.getId());
    }

    private Montado montarSituacao(String companyId, UUID dono, String telefone, SituacaoLead situacao) {
        criarUsuario(UUID.randomUUID(), dono);
        var empresa = CompanyId.reconstituir(companyId);
        var lead = gravarLead(empresa, telefone, situacao, null);
        var etapa = etapa(companyId, "Aberto " + situacao, situacao == SituacaoLead.GANHO ? 4 : 5, false, false);
        var negocio = NegocioFunil.tryCreate(lead.getId(), etapa.getId(), ENTRADA);
        assertTrue(negocio.isOk());
        negocios.salvar(negocio.getValue());
        distribuicao.salvarDono(new DonoLead(lead.getId(), companyId, dono, ENTRADA, false), ENTRADA);
        return new Montado(empresa, lead, etapa.getId());
    }

    private void gravarRoteiro(Montado montado, Boolean decisor, Boolean dor, Boolean usuarios, Boolean prazo,
                               Boolean orcamento, LocalDate demo) {
        fresco();
        var negocio = negocios.buscar(montado.leadId(), montado.empresa()).orElseThrow();
        var atualizado = negocio.comDadosAbertos(decisor, dor, usuarios, prazo, orcamento, demo, null, null, null);
        assertTrue(atualizado.isOk());
        negocios.salvar(atualizado.getValue());
    }

    private Etapa etapa(String companyId, String nome, int ordem, boolean qualificacao, boolean demo) {
        return etapa(companyId, nome, ordem, qualificacao, demo, TipoEtapa.ABERTO);
    }

    private Etapa etapa(String companyId, String nome, int ordem, boolean qualificacao, boolean demo, TipoEtapa tipo) {
        var criada = Etapa.tryCreate(companyId, nome, tipo, ordem, true, null, null, null, qualificacao, demo);
        assertTrue(criada.isOk());
        etapas.salvar(criada.getValue());
        return criada.getValue();
    }

    private Lead gravarLead(CompanyId empresa, String telefone, SituacaoLead situacao, OffsetDateTime deletedAt) {
        var agora = Instant.parse("2026-04-01T12:00:00Z");
        var lead = Lead.reconstituir(UUID.randomUUID(), agora, agora, deletedAt, empresa,
                NomeContato.reconstituir("Ana"), TelefoneBrasil.reconstituir(telefone), null, null,
                OrigemLead.IMPORTACAO, situacao, null, null, false);
        leads.salvar(lead);
        return lead;
    }

    private HistoricoEtapaEntity historico(UUID origem, UUID destino) {
        fresco();
        return em.createQuery("""
                        from HistoricoEtapaEntity h
                        where h.origemEtapaId = :origem and h.destinoEtapaId = :destino
                        """, HistoricoEtapaEntity.class)
                .setParameter("origem", origem)
                .setParameter("destino", destino)
                .getSingleResult();
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

    private record Montado(CompanyId empresa, Lead lead, UUID etapaId) {
        UUID leadId() {
            return lead.getId();
        }
    }
}
