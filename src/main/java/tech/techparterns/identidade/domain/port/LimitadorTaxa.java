package tech.techparterns.identidade.domain.port;

import java.time.Duration;

public interface LimitadorTaxa {

    /** Resultado de uma tentativa de consumo: permitido ou segundos até a janela reabrir. */
    record Resultado(boolean permitido, long retryAfterSegundos) {}

    /** Janela fixa: consome uma unidade da chave. */
    Resultado consumir(String chave, int maximo, Duration janela);
}
