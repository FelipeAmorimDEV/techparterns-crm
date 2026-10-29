package tech.techparterns.funil.domain.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import tech.techparterns.shared.domain.Result;

public final class EntradaEtapa {

    public static final String QUALIFICACAO_INCOMPLETA =
            "Roteiro incompleto não autoriza a entrada nesta etapa";
    public static final String DEMO_OBRIGATORIA = "Data de demo é obrigatória nesta etapa";

    private EntradaEtapa() {
    }

    public static Result<Void> avaliar(boolean exigeQualificacao, boolean exigeDemo,
                                       Boolean decisor, Boolean dorPrincipal, Boolean usuariosPrevistos,
                                       Boolean prazoDecisao, Boolean orcamento, LocalDate dataDemo) {
        List<String> erros = new ArrayList<>();
        if (exigeQualificacao && roteiroIncompleto(decisor, dorPrincipal, usuariosPrevistos, prazoDecisao, orcamento))
            erros.add(QUALIFICACAO_INCOMPLETA);
        if (exigeDemo && dataDemo == null)
            erros.add(DEMO_OBRIGATORIA);
        if (!erros.isEmpty())
            return Result.fail(erros);
        return Result.ok();
    }

    private static boolean roteiroIncompleto(Boolean decisor, Boolean dorPrincipal, Boolean usuariosPrevistos,
                                            Boolean prazoDecisao, Boolean orcamento) {
        return decisor == null || dorPrincipal == null || usuariosPrevistos == null
                || prazoDecisao == null || orcamento == null;
    }
}
