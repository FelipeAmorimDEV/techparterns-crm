package tech.techparterns.distribuicao.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class ValidacaoDistribuicaoTest {

    @Test
    void motivoVazioOuLongoFalhaEAte200Passa() {
        assertTrue(ValidacaoDistribuicao.motivoDevolucao(null).isFailure());
        assertTrue(ValidacaoDistribuicao.motivoDevolucao("  ").isFailure());
        assertTrue(ValidacaoDistribuicao.motivoDevolucao("x".repeat(201)).isFailure());
        assertEquals("ok", ValidacaoDistribuicao.motivoDevolucao(" ok ").getValue());
        assertTrue(ValidacaoDistribuicao.motivoDevolucao("x".repeat(200)).isOk());
    }

    @Test
    void pesoForaDeZeroADezFalhaELimiteZeroEAusentePassam() {
        assertTrue(ValidacaoDistribuicao.peso(-1).isFailure());
        assertTrue(ValidacaoDistribuicao.peso(11).isFailure());
        assertTrue(ValidacaoDistribuicao.peso(0).isOk());
        assertTrue(ValidacaoDistribuicao.peso(10).isOk());
        assertTrue(ValidacaoDistribuicao.limite(null).isOk());
        assertEquals(0, ValidacaoDistribuicao.limite(0).getValue());
        assertTrue(ValidacaoDistribuicao.limite(-1).isFailure());
    }

    @Test
    void prazoMenorQueUmFalhaEExatamenteCincoDiasNaoVence() {
        assertTrue(ValidacaoDistribuicao.diasParaParado(0).isFailure());
        assertTrue(ValidacaoDistribuicao.diasParaParado(1).isOk());
        var t0 = Instant.parse("2026-01-01T00:00:00Z");
        assertFalse(PrazoParado.venceu(t0, t0.plus(java.time.Duration.ofHours(120)), 5));
        assertTrue(PrazoParado.venceu(t0, t0.plus(java.time.Duration.ofHours(120)).plusMillis(1), 5));
    }

    @Test
    void ufGravaMaiusculaECidadeCortaPontas() {
        assertEquals("SP", ValidacaoDistribuicao.uf(" sp ").getValue());
        assertTrue(ValidacaoDistribuicao.uf("S").isFailure());
        assertEquals("São Paulo", ValidacaoDistribuicao.cidade(" São Paulo ").getValue());
        assertNull(ValidacaoDistribuicao.cidade("  ").getValue());
    }
}
