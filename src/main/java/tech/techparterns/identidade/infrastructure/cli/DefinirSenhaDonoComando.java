package tech.techparterns.identidade.infrastructure.cli;

import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import jakarta.enterprise.context.ApplicationScoped;

import tech.techparterns.identidade.application.command.DefinirSenhaDonoCommand;
import tech.techparterns.identidade.application.usecase.DefinirSenhaDonoUseCase;

/**
 * Comando manual {@code definir-senha-dono} (RN-IA-27). A senha NUNCA vem por argumento nem por variável de
 * ambiente: prompt sem eco quando há terminal, entrada padrão quando não há.
 */
@ApplicationScoped
public class DefinirSenhaDonoComando {

    private final DefinirSenhaDonoUseCase useCase;

    public DefinirSenhaDonoComando(DefinirSenhaDonoUseCase useCase) { this.useCase = useCase; }

    /** @return código de saída: 0 em sucesso, diferente de zero em qualquer falha. */
    public int executar(Console console, InputStream entrada, PrintStream saida) {
        String senha;
        try {
            if (console != null) {
                char[] a = console.readPassword("Nova senha do dono: ");
                char[] b = console.readPassword("Confirme a senha: ");
                if (a == null || b == null || !java.util.Arrays.equals(a, b)) {
                    saida.println("As senhas não conferem.");
                    return 2;
                }
                senha = new String(a);
            } else {
                var leitor = new BufferedReader(new InputStreamReader(entrada, StandardCharsets.UTF_8));
                senha = leitor.readLine();
            }
        } catch (IOException e) {
            saida.println("Não foi possível ler a senha.");
            return 2;
        }
        if (senha == null) {
            saida.println("Nenhuma senha informada.");
            return 2;
        }
        var resultado = useCase.execute(new DefinirSenhaDonoCommand(senha));
        if (resultado.isFailure()) {
            saida.println("Senha não definida: " + String.join("; ", resultado.getErrors()));
            return 1;
        }
        saida.println("Senha do dono definida; as sessões anteriores foram revogadas.");
        return 0;
    }
}
