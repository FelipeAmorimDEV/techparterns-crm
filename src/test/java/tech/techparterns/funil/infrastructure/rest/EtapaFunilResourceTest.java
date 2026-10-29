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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import tech.techparterns.funil.domain.model.Etapa;
import tech.techparterns.funil.domain.model.HistoricoEtapa;
import tech.techparterns.funil.domain.model.NegocioFunil;
import tech.techparterns.funil.domain.model.OrdemEtapa;
import tech.techparterns.funil.domain.model.TipoEtapa;
import tech.techparterns.funil.domain.repository.EtapaRepository;
import tech.techparterns.funil.domain.repository.HistoricoEtapaRepository;
import tech.techparterns.funil.domain.repository.NegocioFunilRepository;
import tech.techparterns.lead.domain.model.Lead;
import tech.techparterns.lead.domain.model.NomeContato;
import tech.techparterns.lead.domain.model.TelefoneBrasil;
import tech.techparterns.lead.domain.repository.LeadRepository;
import tech.techparterns.shared.domain.CompanyId;
import tech.techparterns.shared.domain.Result;

@QuarkusTest
class EtapaFunilResourceTest {
    private static final Instant TRANSICAO = Instant.parse("2026-04-01T15:30:00Z");
    @Inject
    EtapaRepository etapas;
    @Inject
    HistoricoEtapaRepository historicos;
    @Inject
    NegocioFunilRepository negocios;
    @Inject
    LeadRepository leads;
    @Inject
    EntityManager em;

    EtapaFunilResourceTest() {
    }

    @Test
    @TestSecurity(user="u", roles={"ADMIN"})
    @JwtSecurity(claims={@Claim(key="companyId", value="funil-t7-aberto")})
    void adminPostAbertoGravaPedidoENasceFinais() {
        CompanyId empresa = CompanyId.reconstituir((String)"funil-t7-aberto");
        this.criar(EtapaFunilResourceTest.corpo("Proposta", "ABERTO", 8, true, 50, "#1A2B3C", 2, false, false));
        this.fresco();
        List<Etapa> lista = this.etapas.listarPorEmpresa(empresa);
        Assertions.assertEquals((int)3, (int)lista.size());
        Assertions.assertEquals((long)1L, (long)lista.stream().filter((Etapa e) -> e.tipo() == TipoEtapa.ABERTO).count());
        Etapa pedida = EtapaFunilResourceTest.porOrdem(lista, 8);
        Assertions.assertEquals((Object)TipoEtapa.ABERTO, (Object)pedida.tipo());
        Assertions.assertTrue((boolean)pedida.ativa());
        Assertions.assertEquals((int)50, (int)pedida.probabilidade().valor());
        Assertions.assertEquals((Object)"#1A2B3C", (Object)pedida.cor().valor());
        Assertions.assertEquals((int)2, (Integer)pedida.sla().valor());
        Assertions.assertFalse((boolean)pedida.exigeQualificacao());
        Assertions.assertFalse((boolean)pedida.exigeDemo());
        EtapaFunilResourceTest.assertNascidaFinal(EtapaFunilResourceTest.unica(lista, TipoEtapa.GANHO));
        EtapaFunilResourceTest.assertNascidaFinal(EtapaFunilResourceTest.unica(lista, TipoEtapa.PERDIDO));
    }

    @Test
    @TestSecurity(user="u", roles={"ADMIN"})
    @JwtSecurity(claims={@Claim(key="companyId", value="funil-t7-ganho")})
    void adminPostGanhoComoPrimeiraOperacao() {
        CompanyId empresa = CompanyId.reconstituir((String)"funil-t7-ganho");
        this.criar(EtapaFunilResourceTest.corpo("Fechamento", "GANHO", 5, true, null, null, null, false, false));
        this.fresco();
        List<Etapa> lista = this.etapas.listarPorEmpresa(empresa);
        Assertions.assertEquals((int)3, (int)lista.size());
        Etapa pedida = EtapaFunilResourceTest.porOrdem(lista, 5);
        Assertions.assertEquals((Object)TipoEtapa.GANHO, (Object)pedida.tipo());
        Assertions.assertTrue((boolean)pedida.ativa());
        Assertions.assertNull((Object)pedida.probabilidade());
        Assertions.assertNull((Object)pedida.cor());
        Assertions.assertTrue((boolean)pedida.sla().vazio());
        Assertions.assertFalse((boolean)pedida.exigeQualificacao());
        Assertions.assertFalse((boolean)pedida.exigeDemo());
        Etapa aberto = EtapaFunilResourceTest.unica(lista, TipoEtapa.ABERTO);
        Assertions.assertNotNull((Object)aberto.probabilidade());
        Assertions.assertEquals((int)0, (int)aberto.probabilidade().valor());
        Assertions.assertTrue((boolean)aberto.sla().vazio());
        EtapaFunilResourceTest.assertNascidaFinal(EtapaFunilResourceTest.unica(lista, TipoEtapa.PERDIDO));
    }

    @Test
    @TestSecurity(user="u", roles={"ADMIN"})
    @JwtSecurity(claims={@Claim(key="companyId", value="funil-t7-perdido")})
    void adminPostPerdidoComoPrimeiraOperacao() {
        CompanyId empresa = CompanyId.reconstituir((String)"funil-t7-perdido");
        this.criar(EtapaFunilResourceTest.corpo("Encerrado", "PERDIDO", 9, true, null, null, null, false, false));
        this.fresco();
        List<Etapa> lista = this.etapas.listarPorEmpresa(empresa);
        Assertions.assertEquals((int)3, (int)lista.size());
        Etapa pedida = EtapaFunilResourceTest.porOrdem(lista, 9);
        Assertions.assertEquals((Object)TipoEtapa.PERDIDO, (Object)pedida.tipo());
        Assertions.assertTrue((boolean)pedida.ativa());
        Assertions.assertNull((Object)pedida.probabilidade());
        Assertions.assertNull((Object)pedida.cor());
        Assertions.assertTrue((boolean)pedida.sla().vazio());
        Assertions.assertFalse((boolean)pedida.exigeQualificacao());
        Assertions.assertFalse((boolean)pedida.exigeDemo());
        Etapa aberto = EtapaFunilResourceTest.unica(lista, TipoEtapa.ABERTO);
        Assertions.assertNotNull((Object)aberto.probabilidade());
        Assertions.assertEquals((int)0, (int)aberto.probabilidade().valor());
        Assertions.assertTrue((boolean)aberto.sla().vazio());
        EtapaFunilResourceTest.assertNascidaFinal(EtapaFunilResourceTest.unica(lista, TipoEtapa.GANHO));
    }

    @Test
    @TestSecurity(user="u", roles={"ADMIN"})
    @JwtSecurity(claims={@Claim(key="companyId", value="funil-t7-invalido")})
    void corProbabilidadeESlaInvalidosNaoGravamEtapa() {
        CompanyId empresa = CompanyId.reconstituir((String)"funil-t7-invalido");
        EtapaFunilResourceTest.postRecusado(EtapaFunilResourceTest.corpo("Contato", "ABERTO", 1, true, 50, "#FFF", 2, false, false), "Cor deve ser # seguido de seis dígitos hexadecimais");
        this.fresco();
        Assertions.assertTrue((boolean)this.etapas.listarPorEmpresa(empresa).isEmpty());
        EtapaFunilResourceTest.postRecusado(EtapaFunilResourceTest.corpo("Contato", "ABERTO", 1, true, 101, "#1A2B3C", 2, false, false), "Probabilidade deve estar entre 0 e 100");
        this.fresco();
        Assertions.assertTrue((boolean)this.etapas.listarPorEmpresa(empresa).isEmpty());
        EtapaFunilResourceTest.postRecusado(EtapaFunilResourceTest.corpo("Contato", "ABERTO", 1, true, 50, "#1A2B3C", 0, false, false), "SLA em dias deve ser um inteiro positivo");
        this.fresco();
        Assertions.assertTrue((boolean)this.etapas.listarPorEmpresa(empresa).isEmpty());
        EtapaFunilResourceTest.postRecusado(EtapaFunilResourceTest.corpo("Contato", "ABERTO", 1, true, 50, "#1A2B3C", -1, false, false), "SLA em dias deve ser um inteiro positivo");
        this.fresco();
        Assertions.assertTrue((boolean)this.etapas.listarPorEmpresa(empresa).isEmpty());
    }

    @Test
    @TestSecurity(user="u", roles={"ADMIN"})
    @JwtSecurity(claims={@Claim(key="companyId", value="funil-t7-prob0")})
    void probabilidadeZeroDeAbertoFicaGravada() {
        CompanyId empresa = CompanyId.reconstituir((String)"funil-t7-prob0");
        this.criar(EtapaFunilResourceTest.corpo("Contato", "ABERTO", 4, true, 0, null, null, false, false));
        this.fresco();
        Etapa pedida = EtapaFunilResourceTest.porOrdem(this.etapas.listarPorEmpresa(empresa), 4);
        Assertions.assertEquals((Object)TipoEtapa.ABERTO, (Object)pedida.tipo());
        Assertions.assertNotNull((Object)pedida.probabilidade());
        Assertions.assertEquals((int)0, (int)pedida.probabilidade().valor());
    }

    @Test
    @TestSecurity(user="u", roles={"ADMIN"})
    @JwtSecurity(claims={@Claim(key="companyId", value="funil-t7-ordem")})
    void ordemRepetidaNoPostPermanece() {
        CompanyId empresa = CompanyId.reconstituir((String)"funil-t7-ordem");
        UUID id = this.criar(EtapaFunilResourceTest.corpo("Existente", "ABERTO", 3, true, 10, null, null, false, false));
        this.fresco();
        List<Etapa> antes = this.etapas.listarPorEmpresa(empresa);
        Assertions.assertEquals((int)3, (int)antes.size());
        Etapa gravada = antes.stream().filter((Etapa e) -> e.getId().equals(id)).findFirst().orElseThrow();
        int ordemAntes = gravada.ordem().valor();
        Assertions.assertEquals((int)3, (int)ordemAntes);
        EtapaFunilResourceTest.postRecusado(EtapaFunilResourceTest.corpo("Nova", "ABERTO", ordemAntes, true, 20, null, null, false, false), "Já existe etapa com essa ordem na empresa");
        this.fresco();
        List<Etapa> depois = this.etapas.listarPorEmpresa(empresa);
        Assertions.assertEquals((int)antes.size(), (int)depois.size());
        Etapa mesma = depois.stream().filter((Etapa e) -> e.getId().equals(id)).findFirst().orElseThrow();
        Assertions.assertEquals((int)ordemAntes, (int)mesma.ordem().valor());
        for (Etapa anterior : antes) {
            Etapa atual = depois.stream().filter((Etapa e) -> e.getId().equals(anterior.getId())).findFirst().orElseThrow();
            Assertions.assertEquals((int)anterior.ordem().valor(), (int)atual.ordem().valor());
        }
    }

    @Test
    @TestSecurity(user="u", roles={"ADMIN"})
    @JwtSecurity(claims={@Claim(key="companyId", value="funil-t7-ganho-dup")})
    void segundaGanhoAtivaNaoNascePerdido() {
        CompanyId empresa = CompanyId.reconstituir((String)"funil-t7-ganho-dup");
        Etapa existente = Etapa.nascida((CompanyId)empresa, (TipoEtapa)TipoEtapa.GANHO, (OrdemEtapa)OrdemEtapa.reconstituir((int)1));
        this.etapas.salvar(existente);
        EtapaFunilResourceTest.postRecusado(EtapaFunilResourceTest.corpo("Outro ganho", "GANHO", 4, true, null, null, null, false, false), "Já existe etapa GANHO ativa");
        this.fresco();
        List<Etapa> lista = this.etapas.listarPorEmpresa(empresa);
        Assertions.assertEquals((int)1, (int)lista.size());
        Assertions.assertEquals((Object)existente.getId(), (Object)((Etapa)lista.getFirst()).getId());
        Assertions.assertEquals((int)1, (int)((Etapa)lista.getFirst()).ordem().valor());
        Assertions.assertEquals((Object)TipoEtapa.GANHO, (Object)((Etapa)lista.getFirst()).tipo());
        Assertions.assertTrue((boolean)lista.stream().noneMatch((Etapa e) -> e.tipo() == TipoEtapa.PERDIDO));
    }

    @Test
    @TestSecurity(user="u", roles={"ADMIN"})
    @JwtSecurity(claims={@Claim(key="companyId", value="funil-t7-perdido-dup")})
    void segundaPerdidoAtivaNaoNasceGanho() {
        CompanyId empresa = CompanyId.reconstituir((String)"funil-t7-perdido-dup");
        Etapa existente = Etapa.nascida((CompanyId)empresa, (TipoEtapa)TipoEtapa.PERDIDO, (OrdemEtapa)OrdemEtapa.reconstituir((int)1));
        this.etapas.salvar(existente);
        EtapaFunilResourceTest.postRecusado(EtapaFunilResourceTest.corpo("Outro perdido", "PERDIDO", 4, true, null, null, null, false, false), "Já existe etapa PERDIDO ativa");
        this.fresco();
        List<Etapa> lista = this.etapas.listarPorEmpresa(empresa);
        Assertions.assertEquals((int)1, (int)lista.size());
        Assertions.assertEquals((Object)existente.getId(), (Object)((Etapa)lista.getFirst()).getId());
        Assertions.assertEquals((int)1, (int)((Etapa)lista.getFirst()).ordem().valor());
        Assertions.assertEquals((Object)TipoEtapa.PERDIDO, (Object)((Etapa)lista.getFirst()).tipo());
        Assertions.assertTrue((boolean)lista.stream().noneMatch((Etapa e) -> e.tipo() == TipoEtapa.GANHO));
    }

    @Test
    @TestSecurity(user="u", roles={"ADMIN"})
    @JwtSecurity(claims={@Claim(key="companyId", value="funil-t7-patch")})
    void adminRenomeiaReordenaEGravaProbabilidadeCorESla() {
        CompanyId empresa = CompanyId.reconstituir((String)"funil-t7-patch");
        UUID id = this.criar(EtapaFunilResourceTest.corpo("Qualificacao", "ABERTO", 8, true, 50, "#1A2B3C", 2, true, true));
        this.fresco();
        Etapa antes = (Etapa)this.etapas.buscarPorId(id, empresa).orElseThrow();
        Assertions.assertEquals((Object)TipoEtapa.ABERTO, (Object)antes.tipo());
        Assertions.assertTrue((boolean)antes.ativa());
        Assertions.assertTrue((boolean)antes.exigeQualificacao());
        Assertions.assertTrue((boolean)antes.exigeDemo());
        ((ValidatableResponse)((Response)RestAssured.given().contentType(ContentType.JSON).body(EtapaFunilResourceTest.corpoAlterar("Renomeada", 6, 40, "#ABCDEF", 5)).when().patch("/funil/etapas/" + String.valueOf(id), new Object[0])).then()).statusCode(204);
        this.fresco();
        Etapa depois = (Etapa)this.etapas.buscarPorId(id, empresa).orElseThrow();
        Assertions.assertEquals((Object)"Renomeada", (Object)depois.nome());
        Assertions.assertEquals((int)6, (int)depois.ordem().valor());
        Assertions.assertEquals((int)40, (int)depois.probabilidade().valor());
        Assertions.assertEquals((Object)"#ABCDEF", (Object)depois.cor().valor());
        Assertions.assertEquals((int)5, (Integer)depois.sla().valor());
        Assertions.assertTrue((boolean)depois.exigeQualificacao());
        Assertions.assertTrue((boolean)depois.exigeDemo());
        Assertions.assertEquals((Object)TipoEtapa.ABERTO, (Object)depois.tipo());
        Assertions.assertTrue((boolean)depois.ativa());
    }

    @Test
    @TestSecurity(user="u", roles={"ADMIN"})
    @JwtSecurity(claims={@Claim(key="companyId", value="funil-t7-patch-ordem")})
    void ordemRepetidaNoPatchPermanece() {
        CompanyId empresa = CompanyId.reconstituir((String)"funil-t7-patch-ordem");
        this.criar(EtapaFunilResourceTest.corpo("Primeira", "ABERTO", 4, true, 10, null, null, false, false));
        UUID segunda = this.criar(EtapaFunilResourceTest.corpo("Segunda", "ABERTO", 7, true, 20, null, null, false, false));
        this.fresco();
        Etapa antes = (Etapa)this.etapas.buscarPorId(segunda, empresa).orElseThrow();
        Assertions.assertEquals((Object)"Segunda", (Object)antes.nome());
        Assertions.assertEquals((int)7, (int)antes.ordem().valor());
        ((ValidatableResponse)((ValidatableResponse)((Response)RestAssured.given().contentType(ContentType.JSON).body(EtapaFunilResourceTest.corpoAlterar("Nao grava", 4, 30, null, null)).when().patch("/funil/etapas/" + String.valueOf(segunda), new Object[0])).then()).statusCode(400)).body("mensagem", CoreMatchers.is((Object)"Já existe etapa com essa ordem na empresa"), new Object[0]);
        this.fresco();
        Etapa depois = (Etapa)this.etapas.buscarPorId(segunda, empresa).orElseThrow();
        Assertions.assertEquals((Object)"Segunda", (Object)depois.nome());
        Assertions.assertEquals((int)7, (int)depois.ordem().valor());
        Assertions.assertEquals((int)4, (int)EtapaFunilResourceTest.porOrdem(this.etapas.listarPorEmpresa(empresa), 4).ordem().valor());
    }

    @Test
    @TestSecurity(user="v", roles={"SELLER"})
    @JwtSecurity(claims={@Claim(key="companyId", value="funil-t7-seller")})
    void vendedorNaoAlteraEtapa() {
        CompanyId empresa = CompanyId.reconstituir((String)"funil-t7-seller");
        ((ValidatableResponse)((Response)RestAssured.given().contentType(ContentType.JSON).body(EtapaFunilResourceTest.corpo("Nova", "ABERTO", 8, true, 50, "#1A2B3C", 2, false, false)).when().post("/funil/etapas", new Object[0])).then()).statusCode(403);
        this.fresco();
        Assertions.assertTrue((boolean)this.etapas.listarPorEmpresa(empresa).isEmpty());
        Etapa etapa = (Etapa)Etapa.tryCreate((String)empresa.valor(), (String)"Parada", (TipoEtapa)TipoEtapa.ABERTO, (Integer)1, (boolean)true, (Integer)15, (String)"#445566", (Integer)4, (boolean)true, (boolean)true).getValue();
        this.etapas.salvar(etapa);
        ((ValidatableResponse)((Response)RestAssured.given().contentType(ContentType.JSON).body(EtapaFunilResourceTest.corpoAlterar("Mudou", 2, 90, "#FFFFFF", 9)).when().patch("/funil/etapas/" + String.valueOf(etapa.getId()), new Object[0])).then()).statusCode(403);
        EtapaFunilResourceTest.desativar(etapa.getId()).statusCode(403);
        ((ValidatableResponse)((Response)RestAssured.given().when().delete("/funil/etapas/" + String.valueOf(etapa.getId()), new Object[0])).then()).statusCode(403);
        this.fresco();
        Assertions.assertEquals((int)1, (int)this.etapas.listarPorEmpresa(empresa).size());
        Etapa lida = (Etapa)this.etapas.buscarPorId(etapa.getId(), empresa).orElseThrow();
        Assertions.assertEquals((Object)"Parada", (Object)lida.nome());
        Assertions.assertEquals((Object)TipoEtapa.ABERTO, (Object)lida.tipo());
        Assertions.assertEquals((int)1, (int)lida.ordem().valor());
        Assertions.assertTrue((boolean)lida.ativa());
        Assertions.assertEquals((int)15, (int)lida.probabilidade().valor());
        Assertions.assertEquals((Object)"#445566", (Object)lida.cor().valor());
        Assertions.assertEquals((int)4, (Integer)lida.sla().valor());
        Assertions.assertTrue((boolean)lida.exigeQualificacao());
        Assertions.assertTrue((boolean)lida.exigeDemo());
        Assertions.assertNull((Object)lida.getDeletedAt());
    }

    @Test
    @TestSecurity(user="u", roles={"ADMIN"})
    @JwtSecurity(claims={@Claim(key="companyId", value="funil-t7-com-lead")})
    void etapaComLeadNaoSomeEDesativaQuandoVazia() {
        CompanyId empresa = CompanyId.reconstituir((String)"funil-t7-com-lead");
        UUID comLead = this.criar(EtapaFunilResourceTest.corpo("Contato", "ABERTO", 4, true, 10, null, null, false, false));
        UUID outraAberta = this.criar(EtapaFunilResourceTest.corpo("Visita", "ABERTO", 6, true, 20, null, null, false, false));
        Lead lead = this.gravarLead(empresa, "Helena", "+5511900007101");
        UUID usuario = UUID.randomUUID();
        HistoricoEtapa historico = this.gravarHistorico(comLead, usuario);
        this.gravarNegocio(lead.getId(), comLead);
        this.fresco();
        Assertions.assertTrue((boolean)((Etapa)this.etapas.buscarPorId(outraAberta, empresa).orElseThrow()).ativa());
        ((ValidatableResponse)((ValidatableResponse)((Response)RestAssured.given().when().delete("/funil/etapas/" + String.valueOf(comLead), new Object[0])).then()).statusCode(400)).body("mensagem", CoreMatchers.is((Object)"Etapa com lead não é apagada"), new Object[0]);
        ((ValidatableResponse)EtapaFunilResourceTest.desativar(comLead).statusCode(400)).body("mensagem", CoreMatchers.is((Object)"Etapa com lead não é desativada"), new Object[0]);
        this.fresco();
        Etapa ocupada = (Etapa)this.etapas.buscarPorId(comLead, empresa).orElseThrow();
        Assertions.assertTrue((boolean)ocupada.ativa());
        Assertions.assertNull((Object)ocupada.getDeletedAt());
        this.assertHistorico(historico.getId(), empresa, comLead, usuario);
        this.apagarNegocio(lead.getId());
        EtapaFunilResourceTest.desativar(comLead).statusCode(204);
        this.fresco();
        Etapa desativada = (Etapa)this.etapas.buscarPorId(comLead, empresa).orElseThrow();
        Assertions.assertFalse((boolean)desativada.ativa());
        this.assertHistorico(historico.getId(), empresa, comLead, usuario);
    }

    @Test
    @TestSecurity(user="u", roles={"ADMIN"})
    @JwtSecurity(claims={@Claim(key="companyId", value="funil-t7-ultimas")})
    void ultimaAbertaEUnicasFinaisNaoDesativam() {
        CompanyId empresa = CompanyId.reconstituir((String)"funil-t7-ultimas");
        this.criar(EtapaFunilResourceTest.corpo("Contato", "ABERTO", 4, true, 10, null, null, false, false));
        this.fresco();
        List<Etapa> lista = this.etapas.listarPorEmpresa(empresa);
        Etapa aberta = EtapaFunilResourceTest.unica(lista, TipoEtapa.ABERTO);
        Etapa ganho = EtapaFunilResourceTest.unica(lista, TipoEtapa.GANHO);
        Etapa perdido = EtapaFunilResourceTest.unica(lista, TipoEtapa.PERDIDO);
        ((ValidatableResponse)EtapaFunilResourceTest.desativar(aberta.getId()).statusCode(400)).body("mensagem", CoreMatchers.is((Object)"A última etapa ABERTO ativa não é desativada"), new Object[0]);
        ((ValidatableResponse)EtapaFunilResourceTest.desativar(ganho.getId()).statusCode(400)).body("mensagem", CoreMatchers.is((Object)"A única etapa GANHO ativa não é desativada"), new Object[0]);
        ((ValidatableResponse)EtapaFunilResourceTest.desativar(perdido.getId()).statusCode(400)).body("mensagem", CoreMatchers.is((Object)"A única etapa PERDIDO ativa não é desativada"), new Object[0]);
        this.fresco();
        Assertions.assertTrue((boolean)((Etapa)this.etapas.buscarPorId(aberta.getId(), empresa).orElseThrow()).ativa());
        Assertions.assertTrue((boolean)((Etapa)this.etapas.buscarPorId(ganho.getId(), empresa).orElseThrow()).ativa());
        Assertions.assertTrue((boolean)((Etapa)this.etapas.buscarPorId(perdido.getId(), empresa).orElseThrow()).ativa());
    }

    @Test
    @TestSecurity(user="u", roles={"ADMIN"})
    @JwtSecurity(claims={@Claim(key="companyId", value="funil-t7-apaga")})
    void apagarSemLeadConservaHistoricoENaoTemCascade() {
        CompanyId empresa = CompanyId.reconstituir((String)"funil-t7-apaga");
        UUID id = this.criar(EtapaFunilResourceTest.corpo("Contato", "ABERTO", 4, true, 10, null, null, false, false));
        UUID usuario = UUID.randomUUID();
        HistoricoEtapa historico = this.gravarHistorico(id, usuario);
        ((ValidatableResponse)((Response)RestAssured.given().when().delete("/funil/etapas/" + String.valueOf(id), new Object[0])).then()).statusCode(204);
        this.fresco();
        Etapa apagada = (Etapa)this.etapas.buscarPorId(id, empresa).orElseThrow();
        Assertions.assertNotNull((Object)apagada.getDeletedAt());
        this.assertHistorico(historico.getId(), empresa, id, usuario);
        List<String> tipos = this.confdeltypeDasFksDoHistorico();
        Assertions.assertFalse((boolean)tipos.isEmpty());
        for (String tipo : tipos) {
            Assertions.assertNotEquals((Object)"c", (Object)tipo);
        }
    }

    @Test
    @TestSecurity(user="u", roles={"ADMIN"})
    @JwtSecurity(claims={@Claim(key="companyId", value="funil-t7-alheia")})
    void etapaDeOutraEmpresaNaoMuda() {
        CompanyId outra = CompanyId.reconstituir((String)"funil-t7-alheia-fora");
        Etapa etapa = (Etapa)Etapa.tryCreate((String)outra.valor(), (String)"Deles", (TipoEtapa)TipoEtapa.ABERTO, (Integer)2, (boolean)true, (Integer)20, (String)"#112233", (Integer)3, (boolean)true, (boolean)false).getValue();
        this.etapas.salvar(etapa);
        ((ValidatableResponse)((ValidatableResponse)((Response)RestAssured.given().contentType(ContentType.JSON).body(EtapaFunilResourceTest.corpoAlterar("Invadiu", 8, 1, "#ABCDEF", 9)).when().patch("/funil/etapas/" + String.valueOf(etapa.getId()), new Object[0])).then()).statusCode(400)).body("mensagem", CoreMatchers.is((Object)"Etapa não encontrada"), new Object[0]);
        ((ValidatableResponse)((ValidatableResponse)((Response)RestAssured.given().when().delete("/funil/etapas/" + String.valueOf(etapa.getId()), new Object[0])).then()).statusCode(400)).body("mensagem", CoreMatchers.is((Object)"Etapa não encontrada"), new Object[0]);
        this.fresco();
        Etapa lida = (Etapa)this.etapas.buscarPorId(etapa.getId(), outra).orElseThrow();
        Assertions.assertEquals((Object)"Deles", (Object)lida.nome());
        Assertions.assertEquals((Object)TipoEtapa.ABERTO, (Object)lida.tipo());
        Assertions.assertEquals((int)2, (int)lida.ordem().valor());
        Assertions.assertTrue((boolean)lida.ativa());
        Assertions.assertEquals((int)20, (int)lida.probabilidade().valor());
        Assertions.assertEquals((Object)"#112233", (Object)lida.cor().valor());
        Assertions.assertEquals((int)3, (Integer)lida.sla().valor());
        Assertions.assertTrue((boolean)lida.exigeQualificacao());
        Assertions.assertFalse((boolean)lida.exigeDemo());
        Assertions.assertNull((Object)lida.getDeletedAt());
    }

    private UUID criar(String corpo) {
        String id = (String)((ValidatableResponse)((ValidatableResponse)((Response)RestAssured.given().contentType(ContentType.JSON).body(corpo).when().post("/funil/etapas", new Object[0])).then()).statusCode(201)).extract().path("id", new String[0]);
        return UUID.fromString(id);
    }

    private static ValidatableResponse desativar(UUID id) {
        return (ValidatableResponse)((Response)RestAssured.given().contentType(ContentType.JSON).when().post("/funil/etapas/" + String.valueOf(id) + "/desativar", new Object[0])).then();
    }

    private static void postRecusado(String corpo, String mensagem) {
        ((ValidatableResponse)((ValidatableResponse)((Response)RestAssured.given().contentType(ContentType.JSON).body(corpo).when().post("/funil/etapas", new Object[0])).then()).statusCode(400)).body("mensagem", CoreMatchers.is((Object)mensagem), new Object[0]);
    }

    private static String corpo(String nome, String tipo, int ordem, boolean ativa, Integer probabilidade, String cor, Integer slaDias, boolean exigeQualificacao, boolean exigeDemo) {
        return "{\"nome\":\"" + nome + "\",\"tipo\":\"" + tipo + "\",\"ordem\":" + ordem + ",\"ativa\":" + ativa + ",\"probabilidade\":" + EtapaFunilResourceTest.json(probabilidade) + ",\"cor\":" + EtapaFunilResourceTest.json(cor) + ",\"slaDias\":" + EtapaFunilResourceTest.json(slaDias) + ",\"exigeQualificacao\":" + exigeQualificacao + ",\"exigeDemo\":" + exigeDemo + "}";
    }

    private static String corpoAlterar(String nome, int ordem, Integer probabilidade, String cor, Integer slaDias) {
        return "{\"nome\":\"" + nome + "\",\"ordem\":" + ordem + ",\"probabilidade\":" + EtapaFunilResourceTest.json(probabilidade) + ",\"cor\":" + EtapaFunilResourceTest.json(cor) + ",\"slaDias\":" + EtapaFunilResourceTest.json(slaDias) + "}";
    }

    private static String json(Integer valor) {
        return valor == null ? "null" : valor.toString();
    }

    private static String json(String valor) {
        return valor == null ? "null" : "\"" + valor + "\"";
    }

    private Lead gravarLead(CompanyId empresa, String nome, String telefone) {
        Result criado = Lead.tryCreate((CompanyId)empresa, (NomeContato)NomeContato.reconstituir((String)nome), (TelefoneBrasil)TelefoneBrasil.reconstituir((String)telefone), null, null);
        Assertions.assertTrue((boolean)criado.isOk());
        this.leads.salvar((Lead)criado.getValue());
        return (Lead)criado.getValue();
    }

    private void gravarNegocio(UUID leadId, UUID etapaId) {
        Result negocio = NegocioFunil.tryCreate((UUID)leadId, (UUID)etapaId, (Instant)TRANSICAO);
        Assertions.assertTrue((boolean)negocio.isOk());
        this.negocios.salvar((NegocioFunil)negocio.getValue());
    }

    private HistoricoEtapa gravarHistorico(UUID destino, UUID userId) {
        Result historico = HistoricoEtapa.tryCreate(null, (UUID)destino, (UUID)userId, (Instant)TRANSICAO);
        Assertions.assertTrue((boolean)historico.isOk());
        this.historicos.salvar((HistoricoEtapa)historico.getValue());
        return (HistoricoEtapa)historico.getValue();
    }

    private void assertHistorico(UUID historicoId, CompanyId empresa, UUID destino, UUID userId) {
        HistoricoEtapa lido = (HistoricoEtapa)this.historicos.buscarPorId(historicoId, empresa).orElseThrow();
        Assertions.assertEquals((Object)historicoId, (Object)lido.getId());
        Assertions.assertNull((Object)lido.origemEtapaId());
        Assertions.assertEquals((Object)destino, (Object)lido.destinoEtapaId());
        Assertions.assertEquals((Object)userId, (Object)lido.userId());
        Assertions.assertEquals((long)TRANSICAO.toEpochMilli(), (long)lido.quando().toEpochMilli());
    }

    private void apagarNegocio(UUID leadId) {
        QuarkusTransaction.requiringNew().run(() -> {
            int apagados = this.em.createNativeQuery("delete from funil_negocio where lead_id = :id").setParameter("id", (Object)leadId).executeUpdate();
            Assertions.assertEquals((int)1, (int)apagados);
            this.em.clear();
        });
    }

    private List<String> confdeltypeDasFksDoHistorico() {
        return (List)QuarkusTransaction.requiringNew().call(() -> {
            List linhas = this.em.createNativeQuery("select c.confdeltype\nfrom pg_constraint c\njoin pg_class t on t.oid = c.conrelid\nwhere t.relname = 'funil_historico_etapa'\n  and c.contype = 'f'\n").getResultList();
            return linhas.stream().map(EtapaFunilResourceTest::confdeltype).toList();
        });
    }

    private static String confdeltype(Object valor) {
        if (valor instanceof Character) {
            Character caractere = (Character)valor;
            return caractere.toString();
        }
        return valor.toString().trim();
    }

    private static void assertNascidaFinal(Etapa etapa) {
        Assertions.assertTrue((boolean)etapa.ativa());
        Assertions.assertNull((Object)etapa.probabilidade());
        Assertions.assertTrue((boolean)etapa.sla().vazio());
        Assertions.assertNull((Object)etapa.cor());
        Assertions.assertFalse((boolean)etapa.exigeQualificacao());
        Assertions.assertFalse((boolean)etapa.exigeDemo());
    }

    private static Etapa unica(List<Etapa> lista, TipoEtapa tipo) {
        List<Etapa> encontradas = lista.stream().filter((Etapa e) -> e.tipo() == tipo).toList();
        Assertions.assertEquals((int)1, (int)encontradas.size(), (String)tipo.name());
        return encontradas.getFirst();
    }

    private static Etapa porOrdem(List<Etapa> lista, int ordem) {
        List<Etapa> encontradas = lista.stream().filter((Etapa e) -> e.ordem().valor() == ordem).toList();
        Assertions.assertEquals((int)1, (int)encontradas.size());
        return encontradas.getFirst();
    }

    private void fresco() {
        QuarkusTransaction.requiringNew().run(() -> ((EntityManager)this.em).clear());
    }
}
