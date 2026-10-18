package tech.techparterns.identidade.application.service;

import java.time.Clock;
import java.time.Duration;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;

import tech.techparterns.identidade.domain.model.EnvioEmail;
import tech.techparterns.identidade.domain.model.TipoTokenSenha;
import tech.techparterns.identidade.domain.model.TokenSenha;
import tech.techparterns.identidade.domain.model.UsuarioEmpresa;
import tech.techparterns.identidade.domain.port.GeradorTokenOpaco;
import tech.techparterns.identidade.domain.repository.EnvioEmailRepository;
import tech.techparterns.identidade.domain.repository.TokenSenhaRepository;
import tech.techparterns.identidade.infrastructure.config.IdentidadeConfig;
import tech.techparterns.shared.domain.exception.LimiteExcedidoException;

/**
 * Gera o token de senha (SETUP ou RESET), invalida os anteriores, registra o envio e dispara o evento que será
 * observado só após o commit. Deve ser chamado dentro de uma transação do chamador.
 */
@ApplicationScoped
public class NotificadorSenha {

    private final TokenSenhaRepository tokens;
    private final EnvioEmailRepository envios;
    private final GeradorTokenOpaco gerador;
    private final Event<EmailParaEnviar> evento;
    private final Clock clock;
    private final IdentidadeConfig config;

    public NotificadorSenha(TokenSenhaRepository tokens, EnvioEmailRepository envios, GeradorTokenOpaco gerador,
                            Event<EmailParaEnviar> evento, Clock clock, IdentidadeConfig config) {
        this.tokens = tokens;
        this.envios = envios;
        this.gerador = gerador;
        this.evento = evento;
        this.clock = clock;
        this.config = config;
    }

    /** Reenvio manual: intervalo mínimo entre envios SETUP ao mesmo usuário (60 s, DEL-03). */
    public void exigirIntervaloDeReenvio(UUID usuarioId) {
        var ultimo = envios.ultimoEnvioSetupEm(usuarioId);
        if (ultimo.isEmpty())
            return;
        var liberaEm = ultimo.get().plus(config.rate().reenvioIntervalo());
        var agora = clock.instant();
        if (agora.isBefore(liberaEm))
            throw new LimiteExcedidoException("Aguarde antes de reenviar o e-mail.",
                    Duration.between(agora, liberaEm).toSeconds() + 1);
    }

    public void notificar(UsuarioEmpresa usuario, String nomeEmpresa, TipoTokenSenha tipo) {
        var agora = clock.instant();
        tokens.invalidarPendentesDoUsuario(usuario.getId(), agora);
        String plano = gerador.gerar();
        tokens.salvar(TokenSenha.criar(usuario.getId(), tipo, gerador.hash(plano), agora));
        var envio = EnvioEmail.pendente(usuario.getId(), tipo, agora);
        envios.salvar(envio);
        evento.fire(new EmailParaEnviar(envio.getId(), usuario.email().valor(), usuario.nome().valor(),
                nomeEmpresa, tipo, plano));
    }
}
