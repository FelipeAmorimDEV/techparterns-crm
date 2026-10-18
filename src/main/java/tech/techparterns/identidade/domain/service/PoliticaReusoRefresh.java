package tech.techparterns.identidade.domain.service;

import java.time.Duration;
import java.time.Instant;

/** RN-IA-16: reuso de refresh já rotacionado é tolerado por 10 s; depois disso revoga a família. */
public final class PoliticaReusoRefresh {

    public enum Decisao { TOLERADO, REUSO_REVOGAR_FAMILIA }

    private final Duration tolerancia;

    public PoliticaReusoRefresh(Duration tolerancia) { this.tolerancia = tolerancia; }

    public Decisao avaliar(Instant rotacionadoEm, Instant agora) {
        return Duration.between(rotacionadoEm, agora).compareTo(tolerancia) <= 0
                ? Decisao.TOLERADO
                : Decisao.REUSO_REVOGAR_FAMILIA;
    }
}
