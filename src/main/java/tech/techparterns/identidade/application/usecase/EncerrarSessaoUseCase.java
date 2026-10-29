package tech.techparterns.identidade.application.usecase;

import java.time.Clock;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import tech.techparterns.identidade.application.command.EncerrarSessaoCommand;
import tech.techparterns.identidade.application.service.AuditoriaAcesso;
import tech.techparterns.identidade.domain.port.ContextoRequisicao;
import tech.techparterns.identidade.domain.port.GeradorTokenOpaco;
import tech.techparterns.identidade.domain.repository.RefreshTokenRepository;
import tech.techparterns.shared.domain.Result;
import tech.techparterns.shared.domain.repository.UseCase;

/** Logout idempotente: revoga a família do refresh apresentado; sem cookie é sucesso sem efeito. */
@ApplicationScoped
public class EncerrarSessaoUseCase implements UseCase<EncerrarSessaoCommand, Void> {

    private final RefreshTokenRepository refreshTokens;
    private final GeradorTokenOpaco gerador;
    private final AuditoriaAcesso auditoria;
    private final ContextoRequisicao requisicao;
    private final Clock clock;

    public EncerrarSessaoUseCase(RefreshTokenRepository refreshTokens, GeradorTokenOpaco gerador,
                                 AuditoriaAcesso auditoria, ContextoRequisicao requisicao, Clock clock) {
        this.refreshTokens = refreshTokens;
        this.gerador = gerador;
        this.auditoria = auditoria;
        this.requisicao = requisicao;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Result<Void> execute(EncerrarSessaoCommand cmd) {
        if (cmd.refreshToken() == null || cmd.refreshToken().isBlank())
            return Result.ok();
        refreshTokens.buscarPorHash(gerador.hash(cmd.refreshToken())).ifPresent(token -> {
            refreshTokens.revogarFamilia(token.familiaId(), clock.instant());
            auditoria.registrar("LOGOUT", AuditoriaAcesso.SUCESSO, token.titularTipo(), token.titularId(), null,
                    requisicao.ip(), null);
        });
        return Result.ok();
    }
}
