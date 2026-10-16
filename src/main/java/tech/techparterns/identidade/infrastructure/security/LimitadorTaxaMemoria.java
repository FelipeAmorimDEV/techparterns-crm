package tech.techparterns.identidade.infrastructure.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.enterprise.context.ApplicationScoped;

import tech.techparterns.identidade.domain.port.LimitadorTaxa;

/**
 * Janela fixa em memória (DTP-6). Limitação conhecida: o limite vale por instância da aplicação.
 */
@ApplicationScoped
public class LimitadorTaxaMemoria implements LimitadorTaxa {

    private static final class Janela {
        Instant inicio;
        int contagem;
    }

    private final ConcurrentHashMap<String, Janela> janelas = new ConcurrentHashMap<>();
    private final Clock clock;

    public LimitadorTaxaMemoria(Clock clock) { this.clock = clock; }

    @Override
    public Resultado consumir(String chave, int maximo, Duration janela) {
        Instant agora = clock.instant();
        var j = janelas.computeIfAbsent(chave, k -> new Janela());
        synchronized (j) {
            if (j.inicio == null || !agora.isBefore(j.inicio.plus(janela))) {
                j.inicio = agora;
                j.contagem = 0;
            }
            if (j.contagem >= maximo) {
                long retry = Duration.between(agora, j.inicio.plus(janela)).toSeconds() + 1;
                return new Resultado(false, Math.max(1, retry));
            }
            j.contagem++;
            return new Resultado(true, 0);
        }
    }

    /** Uso em testes. */
    public void limpar() { janelas.clear(); }
}
