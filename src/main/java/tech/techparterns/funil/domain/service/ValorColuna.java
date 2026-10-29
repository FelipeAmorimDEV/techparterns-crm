package tech.techparterns.funil.domain.service;

import java.math.BigDecimal;
import java.util.List;

import tech.techparterns.funil.domain.model.Probabilidade;
import tech.techparterns.funil.domain.model.TipoEtapa;
import tech.techparterns.funil.domain.model.ValorComercial;

public final class ValorColuna {

    private ValorColuna() {
    }

    public static ValorComercial contribuicao(TipoEtapa tipo, ValorComercial estimadoOuNull,
                                              ValorComercial fechadoOuNull, Probabilidade probabilidadeOuNull) {
        return switch (tipo) {
            case ABERTO -> previsto(estimadoOuNull, probabilidadeOuNull);
            case GANHO -> fechadoOuNull == null ? ValorComercial.zero() : fechadoOuNull;
            case PERDIDO -> estimadoOuNull == null ? ValorComercial.zero() : estimadoOuNull;
        };
    }

    public static ValorComercial somar(List<ValorComercial> valores) {
        BigDecimal total = ValorComercial.zero().valor();
        if (valores != null) {
            for (ValorComercial valor : valores) {
                if (valor != null)
                    total = total.add(valor.valor());
            }
        }
        return ValorComercial.de(total);
    }

    private static ValorComercial previsto(ValorComercial estimadoOuNull, Probabilidade probabilidadeOuNull) {
        BigDecimal base = estimadoOuNull == null ? ValorComercial.zero().valor() : estimadoOuNull.valor();
        int fator = probabilidadeOuNull == null ? 0 : probabilidadeOuNull.valor();
        return ValorComercial.de(base.multiply(BigDecimal.valueOf(fator)));
    }
}
