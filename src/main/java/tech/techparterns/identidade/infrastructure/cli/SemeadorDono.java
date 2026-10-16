package tech.techparterns.identidade.infrastructure.cli;

import org.jboss.logging.Logger;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import tech.techparterns.identidade.application.command.SemearDonoCommand;
import tech.techparterns.identidade.application.usecase.SemearDonoUseCase;
import tech.techparterns.identidade.infrastructure.config.IdentidadeConfig;

/** Cria o primeiro dono (sem senha) na subida, se não houver nenhum. Falhas só vão para o log (CA-24). */
@ApplicationScoped
public class SemeadorDono {

    private static final Logger LOG = Logger.getLogger(SemeadorDono.class);

    private final SemearDonoUseCase semear;
    private final IdentidadeConfig config;

    public SemeadorDono(SemearDonoUseCase semear, IdentidadeConfig config) {
        this.semear = semear;
        this.config = config;
    }

    void aoIniciar(@Observes StartupEvent evento) {
        if (!config.owner().semear())
            return;
        try {
            var resultado = semear.execute(new SemearDonoCommand(config.owner().email().orElse(null),
                    config.owner().nome().orElse(null)));
            if (resultado.isFailure())
                LOG.errorf("Dono da plataforma não criado: %s", resultado.getErrors());
            else if (resultado.getValue() == SemearDonoUseCase.Desfecho.CRIADO)
                LOG.info("Dono da plataforma criado sem senha. Defina a senha pelo comando definir-senha-dono.");
        } catch (Exception e) {
            LOG.errorf("Falha ao semear o dono da plataforma: %s", e.getClass().getSimpleName());
        }
    }
}
