package tech.techparterns.identidade.application.usecase;

import java.time.Clock;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import tech.techparterns.identidade.application.command.TrocarSenhaCommand;
import tech.techparterns.identidade.application.service.AuditoriaAcesso;
import tech.techparterns.identidade.domain.model.*;
import tech.techparterns.identidade.domain.port.ContextoRequisicao;
import tech.techparterns.identidade.domain.port.HasherSenha;
import tech.techparterns.identidade.domain.repository.RefreshTokenRepository;
import tech.techparterns.identidade.domain.repository.UsuarioEmpresaRepository;
import tech.techparterns.identidade.infrastructure.security.ContextoAutenticado;
import tech.techparterns.shared.domain.Result;
import tech.techparterns.shared.domain.exception.RecursoNaoEncontradoException;
import tech.techparterns.shared.domain.repository.UseCase;

/** Exige a senha atual; senha atual errada é 400 e não conta para o bloqueio de login (DEL-04). */
@ApplicationScoped
public class TrocarSenhaUseCase implements UseCase<TrocarSenhaCommand, Void> {

    private final UsuarioEmpresaRepository usuarios;
    private final RefreshTokenRepository refreshTokens;
    private final HasherSenha hasher;
    private final AuditoriaAcesso auditoria;
    private final ContextoAutenticado contexto;
    private final ContextoRequisicao requisicao;
    private final Clock clock;

    public TrocarSenhaUseCase(UsuarioEmpresaRepository usuarios, RefreshTokenRepository refreshTokens,
                              HasherSenha hasher, AuditoriaAcesso auditoria, ContextoAutenticado contexto,
                              ContextoRequisicao requisicao, Clock clock) {
        this.usuarios = usuarios;
        this.refreshTokens = refreshTokens;
        this.hasher = hasher;
        this.auditoria = auditoria;
        this.contexto = contexto;
        this.requisicao = requisicao;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Result<Void> execute(TrocarSenhaCommand cmd) {
        var usuario = usuarios.buscarPorId(contexto.userId(), contexto.empresaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException(ErroIdentidade.USUARIO_NAO_ENCONTRADO.mensagem()));
        if (usuario.hashSenha() == null || SenhaPlana.excedeLimite(cmd.senhaAtual())
                || !hasher.confere(cmd.senhaAtual(), usuario.hashSenha()))
            return Result.fail(ErroIdentidade.SENHA_ATUAL_INCORRETA.mensagem());
        var nova = SenhaPlana.tryCreate(cmd.novaSenha());
        if (nova.isFailure())
            return Result.propagate(nova);
        usuario.definirSenha(hasher.hash(nova.getValue()));
        usuarios.salvar(usuario);
        refreshTokens.revogarTodasDoTitular(TitularTipo.APP, usuario.getId(), clock.instant());
        auditoria.registrar("TROCA_SENHA", AuditoriaAcesso.SUCESSO, TitularTipo.APP, usuario.getId(),
                usuario.empresaId(), requisicao.ip(), null);
        return Result.ok();
    }
}
