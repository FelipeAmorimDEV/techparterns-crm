package tech.techparterns.identidade.infrastructure.cli;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import jakarta.inject.Inject;

/**
 * Ponto de entrada. Sem argumentos, sobe o servidor normalmente. Com {@code definir-senha-dono}, executa o comando
 * e encerra: {@code java -Dquarkus.profile=cli -jar quarkus-run.jar definir-senha-dono} (DTP-7).
 */
@QuarkusMain
public class CrmMain implements QuarkusApplication {

    static final String COMANDO_SENHA_DONO = "definir-senha-dono";

    @Inject
    DefinirSenhaDonoComando definirSenhaDono;

    @Override
    public int run(String... args) {
        if (args.length > 0 && COMANDO_SENHA_DONO.equals(args[0]))
            return definirSenhaDono.executar(System.console(), System.in, System.out);
        Quarkus.waitForExit();
        return 0;
    }
}
