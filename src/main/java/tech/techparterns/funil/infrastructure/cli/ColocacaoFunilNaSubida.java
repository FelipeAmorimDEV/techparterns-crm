package tech.techparterns.funil.infrastructure.cli;

import org.jboss.logging.Logger;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import tech.techparterns.funil.application.command.ColocarLeadsJaGravadosCommand;
import tech.techparterns.funil.application.usecase.ColocarLeadsJaGravadosUseCase;

/** Coloca, na subida, cada lead já gravado que ainda não tem linha no funil. Falha não derruba a subida. */
@ApplicationScoped
public class ColocacaoFunilNaSubida {

    private static final Logger LOG = Logger.getLogger(ColocacaoFunilNaSubida.class);

    private final ColocarLeadsJaGravadosUseCase colocar;

    public ColocacaoFunilNaSubida(ColocarLeadsJaGravadosUseCase colocar) {
        this.colocar = colocar;
    }

    void aoIniciar(@Observes StartupEvent evento) {
        try {
            var resultado = colocar.execute(new ColocarLeadsJaGravadosCommand());
            if (resultado.isFailure())
                LOG.errorf("Leads já gravados não colocados no funil: %s", resultado.getErrors());
        } catch (Exception e) {
            LOG.errorf("Falha ao colocar leads já gravados no funil: %s", e.getClass().getSimpleName());
        }
    }
}
