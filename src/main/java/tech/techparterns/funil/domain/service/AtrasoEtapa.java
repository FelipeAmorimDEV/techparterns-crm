package tech.techparterns.funil.domain.service;

import java.time.Duration;
import java.time.Instant;

import tech.techparterns.funil.domain.model.SlaDias;

public final class AtrasoEtapa {

    private AtrasoEtapa() {
    }

    public static boolean atrasado(Instant entrada, Instant agora, SlaDias sla) {
        if (sla == null || sla.vazio())
            return false;
        Duration limite = Duration.ofHours(sla.valor().longValue() * 24L);
        return Duration.between(entrada, agora).compareTo(limite) > 0;
    }
}
