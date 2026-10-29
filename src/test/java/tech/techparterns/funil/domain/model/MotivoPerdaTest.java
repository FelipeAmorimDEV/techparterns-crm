package tech.techparterns.funil.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import tech.techparterns.shared.domain.CompanyId;

class MotivoPerdaTest {

    @Test
    void tryCreate() {
        var r = MotivoPerda.tryCreate("emp-1", "Preco", true);
        assertTrue(r.isOk());
        assertEquals("emp-1", r.getValue().companyId().valor());
        assertEquals("Preco", r.getValue().nome());
        assertTrue(r.getValue().ativo());
    }

    @Test
    void nomeEmBrancoFalha() {
        assertEquals(java.util.List.of(MotivoPerda.NOME_OBRIGATORIO),
                MotivoPerda.tryCreate("emp-1", "   ", true).getErrors());
        assertEquals(java.util.List.of(MotivoPerda.NOME_OBRIGATORIO),
                MotivoPerda.tryCreate("emp-1", null, false).getErrors());
    }

    @Test
    void alterarAtivo() {
        var motivo = MotivoPerda.tryCreate("emp-1", "Preco", true).getValue();
        motivo.alterarAtivo(false);
        assertFalse(motivo.ativo());
        motivo.alterarAtivo(true);
        assertTrue(motivo.ativo());
    }

    @Test
    void reconstituirPreservaAuditoria() {
        var id = UUID.randomUUID();
        var criado = Instant.parse("2026-04-01T10:00:00Z");
        var atualizado = Instant.parse("2026-04-02T10:00:00Z");
        var excluido = OffsetDateTime.parse("2026-04-03T10:00:00Z");
        var motivo = MotivoPerda.reconstituir(
                id, criado, atualizado, excluido,
                CompanyId.reconstituir("emp-1"),
                "Preco",
                false);
        assertEquals(id, motivo.getId());
        assertEquals(criado, motivo.getCreatedAt());
        assertEquals(atualizado, motivo.getUpdatedAt());
        assertEquals(excluido, motivo.getDeletedAt());
        assertFalse(motivo.ativo());
    }
}
