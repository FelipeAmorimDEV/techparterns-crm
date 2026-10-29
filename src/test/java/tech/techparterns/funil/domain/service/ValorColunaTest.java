package tech.techparterns.funil.domain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import tech.techparterns.funil.domain.model.Probabilidade;
import tech.techparterns.funil.domain.model.TipoEtapa;
import tech.techparterns.funil.domain.model.ValorComercial;

class ValorColunaTest {

    @Test
    void abertoMultiplicaEstimadoPelaProbabilidade() {
        var estimado = valor("100.00");
        var comCinquenta = ValorColuna.contribuicao(TipoEtapa.ABERTO, estimado, null, probabilidade(50));
        assertEquals(0, comCinquenta.valor().compareTo(new BigDecimal("5000.00")));
        assertEquals(2, comCinquenta.valor().scale());

        var comZero = ValorColuna.contribuicao(TipoEtapa.ABERTO, estimado, null, probabilidade(0));
        assertEquals(0, comZero.valor().compareTo(new BigDecimal("0.00")));
        assertEquals(2, comZero.valor().scale());

        var semEstimado = ValorColuna.contribuicao(TipoEtapa.ABERTO, null, null, probabilidade(50));
        assertEquals(0, semEstimado.valor().compareTo(new BigDecimal("0.00")));
        assertEquals(2, semEstimado.valor().scale());
    }

    @Test
    void ganhoUsaFechadoSemMultiplicar() {
        var vazio = ValorColuna.contribuicao(TipoEtapa.GANHO, valor("100.00"), null, probabilidade(50));
        assertEquals(0, vazio.valor().compareTo(new BigDecimal("0.00")));
        assertEquals(2, vazio.valor().scale());

        var fechado = ValorColuna.contribuicao(TipoEtapa.GANHO, null, valor("10.00"), probabilidade(50));
        assertEquals(0, fechado.valor().compareTo(new BigDecimal("10.00")));
        assertEquals(2, fechado.valor().scale());
    }

    @Test
    void perdidoUsaEstimadoSemMultiplicar() {
        var comEstimado = ValorColuna.contribuicao(TipoEtapa.PERDIDO, valor("10.00"), null, probabilidade(50));
        assertEquals(0, comEstimado.valor().compareTo(new BigDecimal("10.00")));
        assertEquals(2, comEstimado.valor().scale());

        var semEstimado = ValorColuna.contribuicao(TipoEtapa.PERDIDO, null, null, probabilidade(50));
        assertEquals(0, semEstimado.valor().compareTo(new BigDecimal("0.00")));
        assertEquals(2, semEstimado.valor().scale());
    }

    @Test
    void somarListaVaziaNulaEDoisValores() {
        var vazia = ValorColuna.somar(List.of());
        var nula = ValorColuna.somar(null);
        assertEquals(0, vazia.valor().compareTo(new BigDecimal("0.00")));
        assertEquals(2, vazia.valor().scale());
        assertEquals(0, nula.valor().compareTo(new BigDecimal("0.00")));
        assertEquals(2, nula.valor().scale());

        var soma = ValorColuna.somar(List.of(valor("10.00"), valor("40.00")));
        assertEquals(0, soma.valor().compareTo(new BigDecimal("50.00")));
        assertEquals(2, soma.valor().scale());
    }

    private static ValorComercial valor(String texto) {
        return ValorComercial.tryCreate(new BigDecimal(texto)).getValue();
    }

    private static Probabilidade probabilidade(int valor) {
        return Probabilidade.tryCreate(valor).getValue();
    }
}
