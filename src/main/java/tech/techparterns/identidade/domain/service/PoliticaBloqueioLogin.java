package tech.techparterns.identidade.domain.service;

import java.time.Duration;
import java.time.Instant;

/**
 * RN-IA-11 / UD-2 / MN-O4. Par (e-mail, IP): 5 falhas sem janela. E-mail: 20 falhas em 15 min.
 * IP: 50 falhas em 15 min. Cada bloqueio dura 15 min fixos do disparo, sem prolongamento.
 */
public final class PoliticaBloqueioLogin {

    private final int maxFalhasPar;
    private final int maxFalhasEmail;
    private final int maxFalhasIp;
    private final Duration janela;
    private final Duration duracaoBloqueio;

    public PoliticaBloqueioLogin(int maxFalhasPar, int maxFalhasEmail, int maxFalhasIp,
                                 Duration janela, Duration duracaoBloqueio) {
        this.maxFalhasPar = maxFalhasPar;
        this.maxFalhasEmail = maxFalhasEmail;
        this.maxFalhasIp = maxFalhasIp;
        this.janela = janela;
        this.duracaoBloqueio = duracaoBloqueio;
    }

    /** Bloqueado enquanto {@code agora} for anterior ao fim; no instante exato o bloqueio acabou. */
    public boolean estaBloqueado(Instant bloqueadoAte, Instant agora) {
        return bloqueadoAte != null && agora.isBefore(bloqueadoAte);
    }

    public boolean parDeveBloquear(int falhasAposIncremento) { return falhasAposIncremento >= maxFalhasPar; }

    public boolean emailDeveBloquear(long falhasNaJanela) { return falhasNaJanela >= maxFalhasEmail; }

    public boolean ipDeveBloquear(long falhasNaJanela) { return falhasNaJanela >= maxFalhasIp; }

    /** Início da janela deslizante, descartando falhas anteriores ao fim do último bloqueio. */
    public Instant inicioDaJanela(Instant agora, Instant zeradoEm) {
        Instant inicio = agora.minus(janela);
        return zeradoEm != null && zeradoEm.isAfter(inicio) ? zeradoEm : inicio;
    }

    public Instant bloqueadoAte(Instant disparo) { return disparo.plus(duracaoBloqueio); }
}
