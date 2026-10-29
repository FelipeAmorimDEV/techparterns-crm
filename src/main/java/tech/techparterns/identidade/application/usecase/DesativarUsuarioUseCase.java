package tech.techparterns.identidade.application.usecase;

import java.time.Clock;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import tech.techparterns.identidade.application.command.UsuarioIdCommand;
import tech.techparterns.identidade.application.service.AuditoriaAcesso;
import tech.techparterns.identidade.domain.model.*;
import tech.techparterns.identidade.domain.port.ContextoRequisicao;
import tech.techparterns.identidade.domain.repository.*;
import tech.techparterns.identidade.domain.service.PoliticaUltimoAdmin;
import tech.techparterns.identidade.infrastructure.security.ContextoAutenticado;
import tech.techparterns.shared.domain.Result;
import tech.techparterns.shared.domain.exception.RecursoNaoEncontradoException;
import tech.techparterns.shared.domain.repository.UseCase;

/**
 * Desativa usuário da empresa (inclusive outros admins e o próprio admin), sujeito à RN-IA-10: as linhas de admin
 * são bloqueadas ANTES de ler o alvo, na mesma transação. Não escreve em {@code lead} (RN-IA-09).
 */
@ApplicationScoped
public class DesativarUsuarioUseCase implements UseCase<UsuarioIdCommand, Void> {

    private final UsuarioEmpresaRepository usuarios;
    private final TokenSenhaRepository tokens;
    private final RefreshTokenRepository refreshTokens;
    private final AuditoriaAcesso auditoria;
    private final ContextoAutenticado contexto;
    private final ContextoRequisicao requisicao;
    private final Clock clock;

    public DesativarUsuarioUseCase(UsuarioEmpresaRepository usuarios, TokenSenhaRepository tokens,
                                   RefreshTokenRepository refreshTokens, AuditoriaAcesso auditoria,
                                   ContextoAutenticado contexto, ContextoRequisicao requisicao, Clock clock) {
        this.usuarios = usuarios;
        this.tokens = tokens;
        this.refreshTokens = refreshTokens;
        this.auditoria = auditoria;
        this.contexto = contexto;
        this.requisicao = requisicao;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Result<Void> execute(UsuarioIdCommand cmd) {
        var empresaId = contexto.empresaId();
        var admins = usuarios.bloquearAdminsDaEmpresa(empresaId);
        var alvo = usuarios.buscarPorId(cmd.id(), empresaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(ErroIdentidade.USUARIO_NAO_ENCONTRADO.mensagem()));
        if (alvo.status() == StatusUsuario.INACTIVE)
            return Result.ok();
        if (!PoliticaUltimoAdmin.podeRemoverAdminAtivo(admins, alvo.getId()))
            return Result.fail(ErroIdentidade.ULTIMO_ADMIN.mensagem());
        var agora = clock.instant();
        alvo.desativar();
        usuarios.salvar(alvo);
        refreshTokens.revogarTodasDoTitular(TitularTipo.APP, alvo.getId(), agora);
        tokens.invalidarPendentesDoUsuario(alvo.getId(), agora);
        auditoria.registrar("USUARIO_DESATIVADO", AuditoriaAcesso.SUCESSO, TitularTipo.APP, contexto.userId(),
                empresaId, requisicao.ip(), null);
        return Result.ok();
    }
}
