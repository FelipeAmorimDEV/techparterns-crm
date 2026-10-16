package tech.techparterns.identidade.application.usecase;

import java.time.Clock;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import tech.techparterns.identidade.application.command.DefinirSenhaDonoCommand;
import tech.techparterns.identidade.application.service.AuditoriaAcesso;
import tech.techparterns.identidade.domain.model.ErroIdentidade;
import tech.techparterns.identidade.domain.model.SenhaPlana;
import tech.techparterns.identidade.domain.model.TitularTipo;
import tech.techparterns.identidade.domain.port.HasherSenha;
import tech.techparterns.identidade.domain.repository.DonoPlataformaRepository;
import tech.techparterns.identidade.domain.repository.RefreshTokenRepository;
import tech.techparterns.shared.domain.Result;
import tech.techparterns.shared.domain.repository.UseCase;

/** Comando manual: define a senha do dono (RN-IA-03 e 72 bytes), revoga as sessões e grava evento (RN-IA-27). */
@ApplicationScoped
public class DefinirSenhaDonoUseCase implements UseCase<DefinirSenhaDonoCommand, Void> {

    private final DonoPlataformaRepository donos;
    private final RefreshTokenRepository refreshTokens;
    private final HasherSenha hasher;
    private final AuditoriaAcesso auditoria;
    private final Clock clock;

    public DefinirSenhaDonoUseCase(DonoPlataformaRepository donos, RefreshTokenRepository refreshTokens,
                                   HasherSenha hasher, AuditoriaAcesso auditoria, Clock clock) {
        this.donos = donos;
        this.refreshTokens = refreshTokens;
        this.hasher = hasher;
        this.auditoria = auditoria;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Result<Void> execute(DefinirSenhaDonoCommand cmd) {
        var dono = donos.buscarPrimeiro().orElse(null);
        if (dono == null)
            return Result.fail(ErroIdentidade.DONO_INEXISTENTE.mensagem());
        var senha = SenhaPlana.tryCreate(cmd.senha());
        if (senha.isFailure())
            return Result.propagate(senha);
        dono.definirSenha(hasher.hash(senha.getValue()));
        donos.salvar(dono);
        refreshTokens.revogarTodasDoTitular(TitularTipo.PLATFORM, dono.getId(), clock.instant());
        auditoria.registrar("SENHA_DONO_DEFINIDA", AuditoriaAcesso.SUCESSO, TitularTipo.PLATFORM, dono.getId(), null,
                "cli", null);
        return Result.ok();
    }
}
