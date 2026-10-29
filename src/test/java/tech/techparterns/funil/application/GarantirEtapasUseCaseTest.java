package tech.techparterns.funil.application;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.jwt.Claim;
import io.quarkus.test.security.jwt.JwtSecurity;
import jakarta.inject.Inject;
import tech.techparterns.funil.application.command.CriarEtapaCommand;
import tech.techparterns.funil.application.usecase.GarantirEtapasUseCase;
import tech.techparterns.funil.domain.model.Etapa;
import tech.techparterns.funil.domain.model.OrdemEtapa;
import tech.techparterns.funil.domain.model.TipoEtapa;
import tech.techparterns.funil.domain.repository.EtapaRepository;
import tech.techparterns.shared.domain.CompanyId;

@QuarkusTest
class GarantirEtapasUseCaseTest {

    @Inject
    GarantirEtapasUseCase caso;

    @Inject
    EtapaRepository etapas;

    @Test
    void empresaSemEtapasGanhaUmaAtivaDeCadaTipo() {
        var empresa = CompanyId.reconstituir("funil-t3-ausentes");

        var resultado = caso.garantirAusentes(empresa);

        assertTrue(resultado.isOk());
        var lista = etapas.listarPorEmpresa(empresa);
        assertEquals(3, lista.size());
        var aberto = unica(lista, TipoEtapa.ABERTO);
        var ganho = unica(lista, TipoEtapa.GANHO);
        var perdido = unica(lista, TipoEtapa.PERDIDO);
        assertNascidaAberta(aberto);
        assertNascidaFinal(ganho);
        assertNascidaFinal(perdido);
        assertEquals(1, aberto.ordem().valor());
        assertEquals(2, ganho.ordem().valor());
        assertEquals(3, perdido.ordem().valor());
    }

    @Test
    @TestSecurity(user = "admin-funil", roles = "ADMIN")
    @JwtSecurity(claims = @Claim(key = "companyId", value = "funil-t3-aberto"))
    void adminCriaAbertoENascemFinais() {
        var empresa = CompanyId.reconstituir("funil-t3-aberto");
        var resultado = caso.execute(new CriarEtapaCommand(
                "Proposta", "ABERTO", 8, true, 50, "#1A2B3C", 2, false, false));

        assertTrue(resultado.isOk());
        var lista = etapas.listarPorEmpresa(empresa);
        assertEquals(3, lista.size());
        assertEquals(1, lista.stream().filter(e -> e.tipo() == TipoEtapa.ABERTO).count());
        var pedida = porOrdem(lista, 8);
        assertEquals(TipoEtapa.ABERTO, pedida.tipo());
        assertTrue(pedida.ativa());
        assertEquals(50, pedida.probabilidade().valor());
        assertEquals("#1A2B3C", pedida.cor().valor());
        assertEquals(2, pedida.sla().valor());
        assertFalse(pedida.exigeQualificacao());
        assertFalse(pedida.exigeDemo());
        assertNascidaFinal(unica(lista, TipoEtapa.GANHO));
        assertNascidaFinal(unica(lista, TipoEtapa.PERDIDO));
    }

    @Test
    @TestSecurity(user = "admin-funil", roles = "ADMIN")
    @JwtSecurity(claims = @Claim(key = "companyId", value = "funil-t3-ganho"))
    void adminCriaGanhoComoPrimeiraOperacao() {
        var empresa = CompanyId.reconstituir("funil-t3-ganho");
        var resultado = caso.execute(new CriarEtapaCommand(
                "Fechamento", "GANHO", 5, true, null, null, null, false, false));

        assertTrue(resultado.isOk());
        var lista = etapas.listarPorEmpresa(empresa);
        assertEquals(3, lista.size());
        var pedida = porOrdem(lista, 5);
        assertEquals(TipoEtapa.GANHO, pedida.tipo());
        assertTrue(pedida.ativa());
        assertNull(pedida.probabilidade());
        assertNull(pedida.cor());
        assertTrue(pedida.sla().vazio());
        assertFalse(pedida.exigeQualificacao());
        assertFalse(pedida.exigeDemo());
        var aberto = unica(lista, TipoEtapa.ABERTO);
        assertNotNull(aberto.probabilidade());
        assertEquals(0, aberto.probabilidade().valor());
        var perdido = unica(lista, TipoEtapa.PERDIDO);
        assertNull(perdido.probabilidade());
    }

    @Test
    @TestSecurity(user = "admin-funil", roles = "ADMIN")
    @JwtSecurity(claims = @Claim(key = "companyId", value = "funil-t3-perdido"))
    void adminCriaPerdidoComoPrimeiraOperacao() {
        var empresa = CompanyId.reconstituir("funil-t3-perdido");
        var resultado = caso.execute(new CriarEtapaCommand(
                "Encerrado", "PERDIDO", 9, true, null, null, null, false, false));

        assertTrue(resultado.isOk());
        var lista = etapas.listarPorEmpresa(empresa);
        assertEquals(3, lista.size());
        var pedida = porOrdem(lista, 9);
        assertEquals(TipoEtapa.PERDIDO, pedida.tipo());
        assertTrue(pedida.ativa());
        assertNull(pedida.probabilidade());
        assertNull(pedida.cor());
        assertTrue(pedida.sla().vazio());
        assertFalse(pedida.exigeQualificacao());
        assertFalse(pedida.exigeDemo());
        var aberto = unica(lista, TipoEtapa.ABERTO);
        assertNotNull(aberto.probabilidade());
        assertEquals(0, aberto.probabilidade().valor());
        var ganho = unica(lista, TipoEtapa.GANHO);
        assertNull(ganho.probabilidade());
    }

    @Test
    @TestSecurity(user = "admin-funil", roles = "ADMIN")
    @JwtSecurity(claims = @Claim(key = "companyId", value = "funil-t3-cor"))
    void corCurtaNaoGravaEtapa() {
        var empresa = CompanyId.reconstituir("funil-t3-cor");
        var resultado = caso.execute(new CriarEtapaCommand(
                "Contato", "ABERTO", 1, true, 50, "#FFF", 2, false, false));

        assertTrue(resultado.isFailure());
        assertTrue(etapas.listarPorEmpresa(empresa).isEmpty());
    }

    @Test
    @TestSecurity(user = "admin-funil", roles = "ADMIN")
    @JwtSecurity(claims = @Claim(key = "companyId", value = "funil-t3-prob"))
    void probabilidadeAcimaDeCemNaoGravaEtapa() {
        var empresa = CompanyId.reconstituir("funil-t3-prob");
        var resultado = caso.execute(new CriarEtapaCommand(
                "Contato", "ABERTO", 1, true, 101, "#1A2B3C", 2, false, false));

        assertTrue(resultado.isFailure());
        assertTrue(etapas.listarPorEmpresa(empresa).isEmpty());
    }

    @Test
    @TestSecurity(user = "admin-funil", roles = "ADMIN")
    @JwtSecurity(claims = @Claim(key = "companyId", value = "funil-t3-sla0"))
    void slaZeroNaoGravaEtapa() {
        var empresa = CompanyId.reconstituir("funil-t3-sla0");
        var resultado = caso.execute(new CriarEtapaCommand(
                "Contato", "ABERTO", 1, true, 50, "#1A2B3C", 0, false, false));

        assertTrue(resultado.isFailure());
        assertTrue(etapas.listarPorEmpresa(empresa).isEmpty());
    }

    @Test
    @TestSecurity(user = "admin-funil", roles = "ADMIN")
    @JwtSecurity(claims = @Claim(key = "companyId", value = "funil-t3-slaneg"))
    void slaNegativoNaoGravaEtapa() {
        var empresa = CompanyId.reconstituir("funil-t3-slaneg");
        var resultado = caso.execute(new CriarEtapaCommand(
                "Contato", "ABERTO", 1, true, 50, "#1A2B3C", -1, false, false));

        assertTrue(resultado.isFailure());
        assertTrue(etapas.listarPorEmpresa(empresa).isEmpty());
    }

    @Test
    @TestSecurity(user = "admin-funil", roles = "ADMIN")
    @JwtSecurity(claims = @Claim(key = "companyId", value = "funil-t3-ganho-dup"))
    void segundaGanhoAtivaNaoNascePerdido() {
        var empresa = CompanyId.reconstituir("funil-t3-ganho-dup");
        var existente = Etapa.nascida(empresa, TipoEtapa.GANHO, OrdemEtapa.reconstituir(1));
        etapas.salvar(existente);

        var resultado = caso.execute(new CriarEtapaCommand(
                "Outro ganho", "GANHO", 4, true, null, null, null, false, false));

        assertTrue(resultado.isFailure());
        var lista = etapas.listarPorEmpresa(empresa);
        assertEquals(1, lista.size());
        assertEquals(existente.getId(), lista.get(0).getId());
        assertEquals(TipoEtapa.GANHO, lista.get(0).tipo());
        assertTrue(lista.stream().noneMatch(e -> e.tipo() == TipoEtapa.PERDIDO));
    }

    @Test
    @TestSecurity(user = "admin-funil", roles = "ADMIN")
    @JwtSecurity(claims = @Claim(key = "companyId", value = "funil-t3-prob0"))
    void probabilidadeZeroDeAbertoFicaGravada() {
        var empresa = CompanyId.reconstituir("funil-t3-prob0");
        var resultado = caso.execute(new CriarEtapaCommand(
                "Contato", "ABERTO", 4, true, 0, null, null, false, false));

        assertTrue(resultado.isOk());
        var pedida = porOrdem(etapas.listarPorEmpresa(empresa), 4);
        assertEquals(TipoEtapa.ABERTO, pedida.tipo());
        assertNotNull(pedida.probabilidade());
        assertEquals(0, pedida.probabilidade().valor());
    }

    @Test
    @TestSecurity(user = "admin-funil", roles = "ADMIN")
    @JwtSecurity(claims = @Claim(key = "companyId", value = "funil-t3-ordem"))
    void ordemRepetidaPermaneceENaoCriaEtapa() {
        var empresa = CompanyId.reconstituir("funil-t3-ordem");
        var existente = Etapa.tryCreate(empresa.valor(), "Existente", TipoEtapa.ABERTO, 3, true,
                null, null, null, false, false).getValue();
        etapas.salvar(existente);

        var antes = etapas.listarPorEmpresa(empresa);
        assertEquals(1, antes.size());
        int ordemAntes = antes.get(0).ordem().valor();

        var resultado = caso.execute(new CriarEtapaCommand(
                "Nova", "GANHO", ordemAntes, true, null, null, null, false, false));

        assertTrue(resultado.isFailure());
        var depois = etapas.listarPorEmpresa(empresa);
        assertEquals(1, depois.size());
        assertEquals(antes.get(0).getId(), depois.get(0).getId());
        assertEquals(ordemAntes, depois.get(0).ordem().valor());
    }

    private static void assertNascidaAberta(Etapa etapa) {
        assertTrue(etapa.ativa());
        assertNotNull(etapa.probabilidade());
        assertEquals(0, etapa.probabilidade().valor());
        assertTrue(etapa.sla().vazio());
        assertNull(etapa.cor());
        assertFalse(etapa.exigeQualificacao());
        assertFalse(etapa.exigeDemo());
    }

    private static void assertNascidaFinal(Etapa etapa) {
        assertTrue(etapa.ativa());
        assertNull(etapa.probabilidade());
        assertTrue(etapa.sla().vazio());
        assertNull(etapa.cor());
        assertFalse(etapa.exigeQualificacao());
        assertFalse(etapa.exigeDemo());
    }

    private static Etapa unica(List<Etapa> lista, TipoEtapa tipo) {
        var encontradas = lista.stream().filter(e -> e.tipo() == tipo).toList();
        assertEquals(1, encontradas.size(), tipo.name());
        return encontradas.get(0);
    }

    private static Etapa porOrdem(List<Etapa> lista, int ordem) {
        var encontradas = lista.stream().filter(e -> e.ordem().valor() == ordem).toList();
        assertEquals(1, encontradas.size());
        return encontradas.get(0);
    }
}
