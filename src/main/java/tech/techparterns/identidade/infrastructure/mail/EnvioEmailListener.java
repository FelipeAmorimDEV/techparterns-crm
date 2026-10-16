package tech.techparterns.identidade.infrastructure.mail;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.jboss.logging.Logger;

import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;

import tech.techparterns.identidade.application.service.EmailParaEnviar;
import tech.techparterns.identidade.domain.port.EnviadorEmail;
import tech.techparterns.identidade.domain.repository.EnvioEmailRepository;
import tech.techparterns.identidade.infrastructure.config.IdentidadeConfig;

/**
 * Envio assíncrono depois do commit, com até 3 tentativas em memória (imediata, +1 min, +5 min; DEL-03).
 * Registra ENVIADO ou FALHA em {@code envio_email}; nunca guarda corpo nem link e nunca os escreve em log.
 */
@ApplicationScoped
public class EnvioEmailListener {

    private static final Logger LOG = Logger.getLogger(EnvioEmailListener.class);

    private final EnviadorEmail enviador;
    private final EnvioEmailRepository envios;
    private final TextosEmail textos;
    private final List<Duration> atrasos;
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2, r -> {
        var t = new Thread(r, "envio-email");
        t.setDaemon(true);
        return t;
    });

    public EnvioEmailListener(EnviadorEmail enviador, EnvioEmailRepository envios, TextosEmail textos,
                              IdentidadeConfig config) {
        this.enviador = enviador;
        this.envios = envios;
        this.textos = textos;
        this.atrasos = config.mail().retryDelays();
    }

    void aoConfirmar(@Observes(during = TransactionPhase.AFTER_SUCCESS) EmailParaEnviar evento) {
        executor.execute(() -> tentar(evento, 0));
    }

    private void tentar(EmailParaEnviar evento, int indice) {
        try {
            enviador.enviar(evento.destinatario(), textos.assunto(evento.tipo()), textos.corpo(evento));
            atualizar(evento, true, false);
        } catch (Exception e) {
            boolean ultima = indice >= atrasos.size();
            LOG.warnf("Falha ao enviar e-mail (envio=%s, tentativa=%d): %s", evento.envioId(), indice + 1,
                    e.getClass().getSimpleName());
            atualizar(evento, false, ultima);
            if (!ultima)
                executor.schedule(() -> tentar(evento, indice + 1), atrasos.get(indice).toMillis(), TimeUnit.MILLISECONDS);
        }
    }

    private void atualizar(EmailParaEnviar evento, boolean sucesso, boolean ultima) {
        try {
            QuarkusTransaction.requiringNew().run(() -> envios.buscarPorId(evento.envioId()).ifPresent(envio -> {
                if (sucesso)
                    envio.registrarSucesso();
                else
                    envio.registrarFalha(ultima);
                envios.salvar(envio);
            }));
        } catch (Exception e) {
            LOG.warnf("Não foi possível atualizar o status do envio %s: %s", evento.envioId(), e.getClass().getSimpleName());
        }
    }

    @PreDestroy
    void encerrar() { executor.shutdownNow(); }
}
