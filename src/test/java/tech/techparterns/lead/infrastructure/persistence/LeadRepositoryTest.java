package tech.techparterns.lead.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.*;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import tech.techparterns.lead.domain.model.*;
import tech.techparterns.lead.domain.repository.LeadRepository;
import tech.techparterns.shared.domain.CompanyId;

@QuarkusTest
class LeadRepositoryTest {

    @Inject
    LeadRepository repo;

    private static Lead novo(CompanyId c, String tel) {
        return Lead.tryCreate(c, NomeContato.reconstituir("Ana"), TelefoneBrasil.reconstituir(tel), null, null).getValue();
    }

    @Test
    void gravaERelePreservandoEstado() {
        var empresa = CompanyId.reconstituir("repo-a");
        var lead = novo(empresa, "+5511988887777");
        repo.salvar(lead);
        var lido = repo.buscarPorId(lead.getId(), empresa).orElseThrow();
        assertEquals("Ana", lido.nome().valor());
        assertEquals("+5511988887777", lido.telefone().valor());
        assertNull(lido.whatsapp());
        assertEquals(OrigemLead.IMPORTACAO, lido.origem());
        assertEquals(lead.getCreatedAt().toEpochMilli(), lido.getCreatedAt().toEpochMilli());
    }

    @Test
    void excluidoVoltaEmBuscarPorIdMasNaoEmListar() {
        var empresa = CompanyId.reconstituir("repo-b");
        var lead = novo(empresa, "+5511988887777");
        repo.salvar(lead);
        lead.excluirLogicamente(OffsetDateTime.now());
        repo.salvar(lead);
        assertTrue(repo.buscarPorId(lead.getId(), empresa).orElseThrow().isDeleted());
        assertTrue(repo.listarNaoExcluidos(empresa).isEmpty());
    }

    @Test
    void outraEmpresaNaoEnxerga() {
        var a = CompanyId.reconstituir("repo-c");
        var b = CompanyId.reconstituir("repo-d");
        var lead = novo(a, "+5511988887777");
        repo.salvar(lead);
        assertTrue(repo.buscarPorId(lead.getId(), b).isEmpty());
        assertTrue(repo.listarNaoExcluidos(b).isEmpty());
        assertEquals(1, repo.listarNaoExcluidos(a).size());
    }
}
