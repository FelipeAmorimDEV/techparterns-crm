package tech.techparterns.distribuicao.domain.model;

import java.time.Duration;
import java.time.Instant;

public final class PrazoParado {

    private PrazoParado() {}

    public static Instant limite(Instant agora, int dias) {
        return agora.minus(Duration.ofHours(dias * 24L));
    }

    /** Exatamente N vezes 24 horas ainda não venceu. */
    public static boolean venceu(Instant atribuidoEm, Instant agora, int dias) {
        return atribuidoEm.isBefore(limite(agora, dias));
    }
}
