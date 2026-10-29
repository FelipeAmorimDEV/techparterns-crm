package tech.techparterns.funil.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import tech.techparterns.shared.domain.CompanyId;
import tech.techparterns.shared.domain.exception.ErroDominio;

class EtapaTest {

    @Test
    void tryCreateValido() {
        var r = Etapa.tryCreate("emp-1", "Qualificacao", TipoEtapa.ABERTO, 1, true, 50, "#1A2B3C", 2, true, false);
        assertTrue(r.isOk());
        var etapa = r.getValue();
        assertEquals("emp-1", etapa.companyId().valor());
        assertEquals("Qualificacao", etapa.nome());
        assertEquals(TipoEtapa.ABERTO, etapa.tipo());
        assertEquals(1, etapa.ordem().valor());
        assertTrue(etapa.ativa());
        assertEquals(50, etapa.probabilidade().valor());
        assertEquals("#1A2B3C", etapa.cor().valor());
        assertEquals(2, etapa.sla().valor());
        assertTrue(etapa.exigeQualificacao());
        assertEquals(false, etapa.exigeDemo());
    }

    @Test
    void camposInvalidosAcumulamNoMesmoResult() {
        var r = Etapa.tryCreate(" ", "   ", null, 0, true, 101, "#FFF", 0, false, false);
        assertTrue(r.isFailure());
        var erros = r.getErrors();
        assertTrue(erros.contains(ErroDominio.EMPRESA_OBRIGATORIA.mensagem()));
        assertTrue(erros.contains(Etapa.NOME_OBRIGATORIO));
        assertTrue(erros.contains(Etapa.TIPO_OBRIGATORIO));
        assertTrue(erros.contains(OrdemEtapa.INVALIDA));
        assertTrue(erros.contains(Probabilidade.FORA_DA_FAIXA));
        assertTrue(erros.contains(CorEtapa.INVALIDA));
        assertTrue(erros.contains(SlaDias.INVALIDO));
    }

    @Test
    void reconstituirPreservaAuditoria() {
        var id = UUID.randomUUID();
        var criado = Instant.parse("2026-03-01T10:00:00Z");
        var atualizado = Instant.parse("2026-03-02T10:00:00Z");
        var excluido = OffsetDateTime.parse("2026-03-03T10:00:00Z");
        var etapa = Etapa.reconstituir(
                id, criado, atualizado, excluido,
                CompanyId.reconstituir("emp-1"),
                "Qualificacao",
                TipoEtapa.ABERTO,
                OrdemEtapa.reconstituir(1),
                true,
                Probabilidade.reconstituir(0),
                null,
                SlaDias.reconstituir(null),
                false,
                false);
        assertEquals(id, etapa.getId());
        assertEquals(criado, etapa.getCreatedAt());
        assertEquals(atualizado, etapa.getUpdatedAt());
        assertEquals(excluido, etapa.getDeletedAt());
        assertNull(etapa.cor());
        assertTrue(etapa.sla().vazio());
    }
}
