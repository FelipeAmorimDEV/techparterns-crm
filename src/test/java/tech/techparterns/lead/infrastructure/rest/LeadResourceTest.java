package tech.techparterns.lead.infrastructure.rest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.jwt.Claim;
import io.quarkus.test.security.jwt.JwtSecurity;
import io.restassured.response.ValidatableResponse;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import tech.techparterns.lead.domain.model.*;
import tech.techparterns.lead.domain.repository.LeadRepository;
import tech.techparterns.shared.domain.CompanyId;

@QuarkusTest
class LeadResourceTest {

    private static final String CAB = "nome_contato,telefone,whatsapp,email\n";
    private static final String TEL = "+55 (11) 98888-7777";
    private static final String TEL_N = "+5511988887777";

    @Inject LeadRepository repo;
    @Inject EntityManager em;

    private static CompanyId emp(String id) { return CompanyId.reconstituir(id); }

    private static ValidatableResponse importar(byte[] csv, String acao) {
        return given().multiPart("arquivo", "leads.csv", csv, "text/csv")
                .multiPart("acao", acao)
                .when().post("/leads/importacoes").then();
    }

    private static ValidatableResponse importar(String csv, String acao) {
        return importar(csv.getBytes(StandardCharsets.UTF_8), acao);
    }

    private static void totais(ValidatableResponse r, int criados, int ignorados, int completados, int mesmoAssim, int rejeitados) {
        r.statusCode(200).body("criados", is(criados)).body("ignorados", is(ignorados))
                .body("completados", is(completados)).body("criadosMesmoAssim", is(mesmoAssim))
                .body("rejeitados", is(rejeitados));
    }

    private Lead semear(String empresa, String nome, String tel, String wpp, String email, Instant criado, Instant atualizado, boolean excluido) {
        var l = Lead.reconstituir(UUID.randomUUID(), criado, atualizado,
                excluido ? java.time.OffsetDateTime.now() : null, emp(empresa), NomeContato.reconstituir(nome),
                tel == null ? null : TelefoneBrasil.reconstituir(tel),
                wpp == null ? null : TelefoneBrasil.reconstituir(wpp),
                email == null ? null : EmailContato.reconstituir(email), OrigemLead.IMPORTACAO);
        repo.salvar(l);
        return l;
    }

    private Lead semear(String empresa, String tel, String wpp, String email) {
        var t = Instant.parse("2026-01-01T00:00:00Z");
        return semear(empresa, "Existente", tel, wpp, email, t, t, false);
    }

    private long contarTodos() {
        return QuarkusTransaction.requiringNew().call(
                () -> em.createQuery("select count(e) from LeadEntity e", Long.class).getSingleResult());
    }

    // CA01, CA02, CA03, CA27
    @Test
    @TestSecurity(user = "u", roles = "admin")
    @JwtSecurity(claims = @Claim(key = "companyId", value = "ca01"))
    void criaLeadNormalizadoComOrigemImportacao() {
        var r = importar(CAB + "Ana," + TEL + ",,\nBia,,+55 (21) 3216-5498,Ana@Empresa.COM\n", "ignorar");
        totais(r, 2, 0, 0, 0, 0);
        r.body("rejeitadas", empty());
        var leads = repo.listarNaoExcluidos(emp("ca01"));
        assertEquals(2, leads.size());
        var ana = leads.stream().filter(l -> l.nome().valor().equals("Ana")).findFirst().orElseThrow();
        assertEquals(TEL_N, ana.telefone().valor());
        assertNull(ana.whatsapp());
        assertNull(ana.email());
        assertEquals(OrigemLead.IMPORTACAO, ana.origem());
        var bia = leads.stream().filter(l -> l.nome().valor().equals("Bia")).findFirst().orElseThrow();
        assertEquals("ana@empresa.com", bia.email().valor());
        assertEquals("+552132165498", bia.whatsapp().valor());
        var json = given().multiPart("arquivo", "l.csv", (CAB + "Cid,,,c@x.com\n").getBytes(StandardCharsets.UTF_8), "text/csv")
                .multiPart("acao", "ignorar").when().post("/leads/importacoes").then().extract().asString();
        assertFalse(json.contains("ganho") || json.contains("perdido") || json.contains("rebanho"));
    }

    // CA03: WhatsApp igual não deduplica
    @Test
    @TestSecurity(user = "u", roles = "admin")
    @JwtSecurity(claims = @Claim(key = "companyId", value = "ca03"))
    void whatsappNaoEChaveDeDuplicidade() {
        semear("ca03", "+5521999999999", "+552132165498", "x@x.com");
        totais(importar(CAB + "Ana,+55 (11) 98888-7777,+55 (21) 3216-5498,y@x.com\n", "ignorar"), 1, 0, 0, 0, 0);
        assertEquals(2, repo.listarNaoExcluidos(emp("ca03")).size());
    }

    // CA04, CA05, CA06, CA10
    @Test
    @TestSecurity(user = "u", roles = "admin")
    @JwtSecurity(claims = @Claim(key = "companyId", value = "ca04"))
    void rejeitaLinhasInvalidasComNumeroEMotivo() {
        var csv = CAB
                + "Valida," + TEL + ",,\n"
                + " ,,,a@x.com\n"
                + "a".repeat(121) + ",,,a@x.com\n"
                + "  " + "b".repeat(120) + "  ,,,b@x.com\n"
                + "Tel ruim,(11) 98888-7777,,ok@x.com\n"
                + "Wpp ruim,,+55 (20) 98888-7777,ok2@x.com\n"
                + "Sem meio,,,\n";
        var r = importar(csv, "ignorar");
        totais(r, 2, 0, 0, 0, 5);
        r.body("rejeitadas", hasSize(5))
                .body("rejeitadas[0].linha", is(3)).body("rejeitadas[0].motivo", is("Nome do contato vazio"))
                .body("rejeitadas[1].linha", is(4)).body("rejeitadas[1].motivo", is("Nome do contato acima de 120 caracteres"))
                .body("rejeitadas[2].linha", is(6)).body("rejeitadas[2].motivo", is("Telefone inválido"))
                .body("rejeitadas[3].linha", is(7)).body("rejeitadas[3].motivo", is("WhatsApp inválido"))
                .body("rejeitadas[4].linha", is(8)).body("rejeitadas[4].motivo", is("Nenhum meio de contato"));
        var leads = repo.listarNaoExcluidos(emp("ca04"));
        assertEquals(2, leads.size());
        assertTrue(leads.stream().anyMatch(l -> l.nome().valor().equals("b".repeat(120))));
    }

    // CA07
    @Test
    @TestSecurity(user = "u", roles = "admin")
    @JwtSecurity(claims = @Claim(key = "companyId", value = "ca07"))
    void tamanhoExatoAceitoEUmByteAMaisRecusado() {
        int limite = 10 * 1024 * 1024;
        String prefixo = "nome_contato,telefone,whatsapp,email,obs\nAna," + TEL + ",,,";
        byte[] exato = (prefixo + "x".repeat(limite - prefixo.length() - 1) + "\n").getBytes(StandardCharsets.UTF_8);
        assertEquals(limite, exato.length);
        totais(importar(exato, "ignorar"), 1, 0, 0, 0, 0);

        byte[] maior = (prefixo + "x".repeat(limite - prefixo.length()) + "\n").getBytes(StandardCharsets.UTF_8);
        assertEquals(limite + 1, maior.length);
        importar(maior, "ignorar").statusCode(400)
                .body("mensagem", is("Arquivo acima do tamanho permitido")).body("codigoErro", is("REQUISICAO_INVALIDA"));
        assertEquals(1, repo.listarNaoExcluidos(emp("ca07")).size());
    }

    // CA08
    @Test
    @TestSecurity(user = "u", roles = "admin")
    @JwtSecurity(claims = @Claim(key = "companyId", value = "ca08"))
    void cinquentaMilLinhasAceitasE50001Recusada() {
        totais(importar(CAB + "\n".repeat(50_000), "ignorar"), 0, 0, 0, 0, 50_000);
        importar(CAB + "\n".repeat(50_001), "ignorar").statusCode(400)
                .body("mensagem", is("Arquivo acima de 50 mil linhas"));
        assertTrue(repo.listarNaoExcluidos(emp("ca08")).isEmpty());
    }

    // CA09
    @Test
    @TestSecurity(user = "u", roles = "admin")
    @JwtSecurity(claims = @Claim(key = "companyId", value = "ca09"))
    void recusasDeArquivoENaoGravam() {
        importar("nome_contato,telefone,whatsapp\nAna,x,y\n", "ignorar").statusCode(400)
                .body("mensagem", is("Cabeçalho obrigatório inválido"));
        importar("nome_contato,telefone,telefone,whatsapp,email\n", "ignorar").statusCode(400)
                .body("mensagem", is("Cabeçalho obrigatório inválido"));
        importar(new byte[] {'a', ',', (byte) 0xC3, (byte) 0x28}, "ignorar").statusCode(400)
                .body("mensagem", is("Arquivo não é UTF-8 válido"));
        importar(CAB + "Ana," + TEL + ",,\n", "outra").statusCode(400)
                .body("mensagem", is("Ação de importação inválida"));
        // coluna extra não recusa e não é gravada
        totais(importar("nome_contato,fazenda,telefone,whatsapp,email\nAna,Sol," + TEL + ",,\n", "ignorar"), 1, 0, 0, 0, 0);
        assertEquals(1, repo.listarNaoExcluidos(emp("ca09")).size());
    }

    // CA28
    @Test
    @TestSecurity(user = "u", roles = "admin")
    @JwtSecurity(claims = @Claim(key = "companyId", value = "ca28"))
    void apenasCabecalho() {
        importar(CAB, "ignorar").statusCode(200).body("rejeitadas", empty())
                .body("criados", is(0)).body("rejeitados", is(0));
        assertTrue(repo.listarNaoExcluidos(emp("ca28")).isEmpty());
    }

    // CA11, CA12
    @Test
    @TestSecurity(user = "u", roles = "admin")
    @JwtSecurity(claims = @Claim(key = "companyId", value = "ca11"))
    void telefoneEscolheLeadEIgnorarConserva() {
        var existente = semear("ca11", TEL_N, null, "velho@x.com");
        totais(importar(CAB + "Ana," + TEL + ",,novo@x.com\n", "ignorar"), 0, 1, 0, 0, 0);
        var leads = repo.listarNaoExcluidos(emp("ca11"));
        assertEquals(1, leads.size());
        assertEquals("velho@x.com", leads.getFirst().email().valor());
        assertEquals(existente.getUpdatedAt(), leads.getFirst().getUpdatedAt());
    }

    // CA13, CA14
    @Test
    @TestSecurity(user = "u", roles = "admin")
    @JwtSecurity(claims = @Claim(key = "companyId", value = "ca13"))
    void completarPreencheSoVazios() {
        var existente = semear("ca13", TEL_N, null, "a@x.com");
        var completo = semear("ca13", "+5521999999999", "+552132165498", "c@x.com");
        var csv = CAB + "Outro Nome," + TEL + ",+55 (31) 98888-7777,zzz@x.com\n"
                + "Outro,+55 (21) 99999-9999,+55 (41) 98888-7777,q@x.com\n";
        totais(importar(csv, "completar_campos_vazios"), 0, 0, 2, 0, 0);
        var leads = repo.listarNaoExcluidos(emp("ca13"));
        assertEquals(2, leads.size());
        var a = leads.stream().filter(l -> l.getId().equals(existente.getId())).findFirst().orElseThrow();
        assertEquals("+5531988887777", a.whatsapp().valor());
        assertEquals("a@x.com", a.email().valor());
        assertEquals("Existente", a.nome().valor());
        assertEquals(OrigemLead.IMPORTACAO, a.origem());
        assertTrue(a.getUpdatedAt().isAfter(existente.getUpdatedAt()));
        var c = leads.stream().filter(l -> l.getId().equals(completo.getId())).findFirst().orElseThrow();
        assertEquals("+552132165498", c.whatsapp().valor());
        assertEquals(completo.getUpdatedAt(), c.getUpdatedAt());
    }

    // CA15
    @Test
    @TestSecurity(user = "u", roles = "admin")
    @JwtSecurity(claims = @Claim(key = "companyId", value = "ca15"))
    void criarMesmoAssim() {
        var existente = semear("ca15", TEL_N, null, null);
        totais(importar(CAB + "Ana," + TEL + ",,\n", "criar_mesmo_assim"), 0, 0, 0, 1, 0);
        var leads = repo.listarNaoExcluidos(emp("ca15"));
        assertEquals(2, leads.size());
        assertTrue(leads.stream().allMatch(l -> l.origem() == OrigemLead.IMPORTACAO));
        var antigo = leads.stream().filter(l -> l.getId().equals(existente.getId())).findFirst().orElseThrow();
        assertEquals(existente.getUpdatedAt(), antigo.getUpdatedAt());
        assertEquals("Existente", antigo.nome().valor());
    }

    // CA16
    @Test
    @TestSecurity(user = "u", roles = "admin")
    @JwtSecurity(claims = @Claim(key = "companyId", value = "ca16"))
    void linhaSeguinteEnxergaLinhaAceita() {
        var csv = CAB + "Ana," + TEL + ",,\nBia," + TEL + ",,\n";
        totais(importar(csv, "ignorar"), 1, 1, 0, 0, 0);
        assertEquals(1, repo.listarNaoExcluidos(emp("ca16")).size());
        totais(importar(csv, "criar_mesmo_assim"), 0, 0, 0, 2, 0);
        assertEquals(3, repo.listarNaoExcluidos(emp("ca16")).size());
    }

    @Test
    @TestSecurity(user = "u", roles = "admin")
    @JwtSecurity(claims = @Claim(key = "companyId", value = "ca16b"))
    void duasLinhasNovasMesmoTelefoneCriarMesmoAssim() {
        totais(importar(CAB + "Ana," + TEL + ",,\nBia," + TEL + ",,\n", "criar_mesmo_assim"), 1, 0, 0, 1, 0);
        assertEquals(2, repo.listarNaoExcluidos(emp("ca16b")).size());
    }

    // CA17
    @Test
    @TestSecurity(user = "u", roles = "admin")
    @JwtSecurity(claims = @Claim(key = "companyId", value = "ca17"))
    void excluidoNaoEDuplicata() {
        var t = Instant.parse("2026-01-01T00:00:00Z");
        var excluido = semear("ca17", "Velho", TEL_N, null, null, t, t, true);
        totais(importar(CAB + "Ana," + TEL + ",,\n", "ignorar"), 1, 0, 0, 0, 0);
        assertEquals(1, repo.listarNaoExcluidos(emp("ca17")).size());
        assertTrue(repo.buscarPorId(excluido.getId(), emp("ca17")).orElseThrow().isDeleted());
    }

    // CA18
    @Test
    @TestSecurity(user = "u", roles = "admin")
    @JwtSecurity(claims = @Claim(key = "companyId", value = "ca18"))
    void desempateEntreAtivos() {
        var t = Instant.parse("2026-01-01T00:00:00Z");
        semear("ca18", "Excluido recente", TEL_N, null, null, t, t.plusSeconds(1000), true);
        var antigo = semear("ca18", "Antigo", TEL_N, null, null, t, t.plusSeconds(10), false);
        var recente = semear("ca18", "Recente", TEL_N, null, null, t.plusSeconds(5), t.plusSeconds(20), false);
        totais(importar(CAB + "Ana," + TEL + ",+55 (31) 98888-7777,\n", "completar_campos_vazios"), 0, 0, 1, 0, 0);
        assertNull(repo.buscarPorId(antigo.getId(), emp("ca18")).orElseThrow().whatsapp());
        assertNotNull(repo.buscarPorId(recente.getId(), emp("ca18")).orElseThrow().whatsapp());
    }

    // CA19, CA22
    @Test
    @TestSecurity(user = "u", roles = "admin")
    @JwtSecurity(claims = @Claim(key = "companyId", value = "ca19"))
    void empresaDoTokenEOutraEmpresaIntocada() {
        var outra = semear("ca19-outra", TEL_N, null, null);
        totais(importar("nome_contato,telefone,whatsapp,email,company_id\nAna," + TEL + ",,,ca19-outra\n", "completar_campos_vazios"),
                1, 0, 0, 0, 0);
        assertEquals(1, repo.listarNaoExcluidos(emp("ca19")).size());
        assertEquals(1, repo.listarNaoExcluidos(emp("ca19-outra")).size());
        assertEquals(outra.getUpdatedAt(), repo.listarNaoExcluidos(emp("ca19-outra")).getFirst().getUpdatedAt());
    }

    // CA20
    @Test
    @TestSecurity(user = "u", roles = "vendedor")
    @JwtSecurity(claims = @Claim(key = "companyId", value = "ca20"))
    void vendedorNaoImportaNemExclui() {
        var lead = semear("ca20", TEL_N, null, null);
        importar(CAB + "Ana," + TEL + ",,\n", "ignorar").statusCode(400)
                .body("mensagem", is("Operação restrita ao admin"));
        given().when().delete("/leads/" + lead.getId()).then().statusCode(400)
                .body("mensagem", is("Operação restrita ao admin"));
        assertEquals(1, repo.listarNaoExcluidos(emp("ca20")).size());
    }

    @Test
    @TestSecurity(user = "u", roles = "")
    @JwtSecurity(claims = @Claim(key = "companyId", value = "ca20b"))
    void papelVazio() {
        importar(CAB + "Ana," + TEL + ",,\n", "ignorar").statusCode(400)
                .body("mensagem", is("Operação restrita ao admin"));
        assertTrue(repo.listarNaoExcluidos(emp("ca20b")).isEmpty());
    }

    @Test
    void semPapel() {
        long antes = contarTodos();
        importar(CAB + "Ana," + TEL + ",,\n", "ignorar").statusCode(400)
                .body("mensagem", is("Operação restrita ao admin"));
        assertEquals(antes, contarTodos());
    }

    // CA21
    @Test
    @TestSecurity(user = "u", roles = "admin")
    void adminSemCompanyId() {
        long antes = contarTodos();
        importar(CAB + "Ana," + TEL + ",,\n", "ignorar").statusCode(500)
                .body("codigoErro", is("ERRO_INTERNO"));
        assertEquals(antes, contarTodos());
    }

    // CA23, CA24
    @Test
    @TestSecurity(user = "u", roles = "admin")
    @JwtSecurity(claims = @Claim(key = "companyId", value = "ca23"))
    void exclusaoLogicaERepetida() {
        var lead = semear("ca23", TEL_N, null, null);
        given().when().delete("/leads/" + lead.getId()).then().statusCode(204);
        var lido = repo.buscarPorId(lead.getId(), emp("ca23")).orElseThrow();
        assertTrue(lido.isDeleted());
        var quando = lido.getDeletedAt();
        given().when().delete("/leads/" + lead.getId()).then().statusCode(204);
        assertTrue(repo.buscarPorId(lead.getId(), emp("ca23")).orElseThrow().isDeleted());
        assertEquals(quando.toInstant().toEpochMilli(),
                repo.buscarPorId(lead.getId(), emp("ca23")).orElseThrow().getDeletedAt().toInstant().toEpochMilli());
    }

    // CA25
    @Test
    @TestSecurity(user = "u", roles = "admin")
    @JwtSecurity(claims = @Claim(key = "companyId", value = "ca25"))
    void identificadorInexistenteOuDeOutraEmpresa() {
        var outra = semear("ca25-outra", TEL_N, null, null);
        given().when().delete("/leads/" + outra.getId()).then().statusCode(400)
                .body("mensagem", is("Lead não encontrado"));
        given().when().delete("/leads/" + UUID.randomUUID()).then().statusCode(400)
                .body("mensagem", is("Lead não encontrado"));
        assertFalse(repo.buscarPorId(outra.getId(), emp("ca25-outra")).orElseThrow().isDeleted());
    }

    // Preserva o endpoint existente
    @Test
    void helloContinuaFuncionando() {
        given().when().get("/hello").then().statusCode(200).body(is("Hello from Quarkus REST"));
    }
}
