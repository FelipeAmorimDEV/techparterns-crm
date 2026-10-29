package tech.techparterns.identidade.application.usecase;

import java.time.Clock;
import java.time.Duration;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import tech.techparterns.identidade.application.command.EditarUsuarioCommand;
import tech.techparterns.identidade.application.dto.UsuarioListadoDto;
import tech.techparterns.identidade.application.service.AuditoriaAcesso;
import tech.techparterns.identidade.application.service.NotificadorSenha;
import tech.techparterns.identidade.domain.model.*;
import tech.techparterns.identidade.domain.port.ContextoRequisicao;
import tech.techparterns.identidade.domain.port.LimitadorTaxa;
import tech.techparterns.identidade.domain.repository.*;
import tech.techparterns.identidade.infrastructure.config.IdentidadeConfig;
import tech.techparterns.identidade.infrastructure.security.ContextoAutenticado;
import tech.techparterns.shared.domain.Result;
import tech.techparterns.shared.domain.exception.DomainException;
import tech.techparterns.shared.domain.exception.ErroHttpException;
import tech.techparterns.shared.domain.exception.LimiteExcedidoException;
import tech.techparterns.shared.domain.exception.RecursoNaoEncontradoException;
import tech.techparterns.shared.domain.repository.UseCase;

/**
 * Edita nome, e-mail e telefone de OUTROS usuários da empresa (o próprio e-mail é imutável, RN-IA-28).
 * Troca de e-mail revoga sessões, invalida tokens e, se PENDING, envia novo SETUP (DEL-15c).
 */
@ApplicationScoped
public class EditarUsuarioUseCase implements UseCase<EditarUsuarioCommand, UsuarioListadoDto> {

    private final UsuarioEmpresaRepository usuarios;
    private final EmpresaRepository empresas;
    private final EmailRegistradoRepository emails;
    private final TokenSenhaRepository tokens;
    private final RefreshTokenRepository refreshTokens;
    private final NotificadorSenha notificador;
    private final AuditoriaAcesso auditoria;
    private final LimitadorTaxa limitador;
    private final ContextoAutenticado contexto;
    private final ContextoRequisicao requisicao;
    private final IdentidadeConfig config;
    private final Clock clock;

    public EditarUsuarioUseCase(UsuarioEmpresaRepository usuarios, EmpresaRepository empresas,
                                EmailRegistradoRepository emails, TokenSenhaRepository tokens,
                                RefreshTokenRepository refreshTokens, NotificadorSenha notificador,
                                AuditoriaAcesso auditoria, LimitadorTaxa limitador, ContextoAutenticado contexto,
                                ContextoRequisicao requisicao, IdentidadeConfig config, Clock clock) {
        this.usuarios = usuarios;
        this.empresas = empresas;
        this.emails = emails;
        this.tokens = tokens;
        this.refreshTokens = refreshTokens;
        this.notificador = notificador;
        this.auditoria = auditoria;
        this.limitador = limitador;
        this.contexto = contexto;
        this.requisicao = requisicao;
        this.config = config;
        this.clock = clock;
    }

    @Override
    @Transactional(dontRollbackOn = DomainException.class)
    public Result<UsuarioListadoDto> execute(EditarUsuarioCommand cmd) {
        var empresaId = contexto.empresaId();
        var adminId = contexto.userId();
        var alvo = usuarios.buscarPorId(cmd.id(), empresaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(ErroIdentidade.USUARIO_NAO_ENCONTRADO.mensagem()));

        NomeUsuario nome = null;
        if (cmd.nome() != null) {
            var r = NomeUsuario.tryCreate(cmd.nome());
            if (r.isFailure()) return Result.propagate(r);
            nome = r.getValue();
        }
        var telefone = TelefoneUsuario.tryCreateOpcional(cmd.telefone());
        if (telefone.isFailure()) return Result.propagate(telefone);
        EmailUsuario novoEmail = null;
        if (cmd.email() != null && !cmd.email().isBlank()) {
            var r = EmailUsuario.tryCreate(cmd.email());
            if (r.isFailure()) return Result.propagate(r);
            if (!r.getValue().equals(alvo.email()))
                novoEmail = r.getValue();
        }

        if (novoEmail != null) {
            if (alvo.getId().equals(adminId))
                return Result.fail(ErroIdentidade.EMAIL_PROPRIO_NAO_ALTERAVEL.mensagem());
            var taxa = limitador.consumir("cadastro:" + adminId, config.rate().cadastroPorHora(), Duration.ofHours(1));
            if (!taxa.permitido())
                throw new LimiteExcedidoException("Limite de cadastros e edições de e-mail por hora excedido.",
                        taxa.retryAfterSegundos());
            if (emails.emUso(novoEmail)) {
                auditoria.registrar("EDICAO_EMAIL_USUARIO", "EMAIL_EM_USO", TitularTipo.APP, adminId, empresaId,
                        requisicao.ip(), null);
                throw new ErroHttpException(400, "EMAIL_INDISPONIVEL", ErroIdentidade.EMAIL_INDISPONIVEL.mensagem());
            }
            var agora = clock.instant();
            alvo.alterarEmail(novoEmail);
            tokens.invalidarPendentesDoUsuario(alvo.getId(), agora);
            refreshTokens.revogarTodasDoTitular(TitularTipo.APP, alvo.getId(), agora);
            auditoria.registrar("EDICAO_EMAIL_USUARIO", AuditoriaAcesso.SUCESSO, TitularTipo.APP, adminId, empresaId,
                    requisicao.ip(), null);
        }
        alvo.editarContato(nome, telefone.getValue());
        usuarios.salvar(alvo);

        if (novoEmail != null && alvo.status() == StatusUsuario.PENDING) {
            var empresa = empresas.buscarPorId(empresaId)
                    .orElseThrow(() -> new RecursoNaoEncontradoException(ErroIdentidade.EMPRESA_NAO_ENCONTRADA.mensagem()));
            notificador.notificar(alvo, empresa.nome().valor(), TipoTokenSenha.SETUP);
        }
        return Result.ok(UsuarioListadoDto.de(alvo, null));
    }
}
