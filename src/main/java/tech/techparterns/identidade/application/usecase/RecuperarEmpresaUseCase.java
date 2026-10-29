package tech.techparterns.identidade.application.usecase;

import java.time.Clock;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import tech.techparterns.identidade.application.command.RecuperarEmpresaCommand;
import tech.techparterns.identidade.application.dto.EmpresaCriadaDto;
import tech.techparterns.identidade.application.service.AuditoriaAcesso;
import tech.techparterns.identidade.application.service.NotificadorSenha;
import tech.techparterns.identidade.domain.model.*;
import tech.techparterns.identidade.domain.port.ContextoRequisicao;
import tech.techparterns.identidade.domain.repository.*;
import tech.techparterns.identidade.domain.service.PoliticaRecuperacaoEmpresa;
import tech.techparterns.identidade.infrastructure.security.ContextoAutenticado;
import tech.techparterns.shared.domain.Result;
import tech.techparterns.shared.domain.exception.ConflitoException;
import tech.techparterns.shared.domain.exception.RecursoNaoEncontradoException;
import tech.techparterns.shared.domain.repository.UseCase;

/**
 * Recupera empresa ACTIVE sem nenhum ADMIN ACTIVE (DEL-12), numa transação que bloqueia a empresa e os admins:
 * TODOS os admins PENDING viram INACTIVE (com SETUP invalidado) e nasce um novo admin PENDING com SETUP de 48 h.
 */
@ApplicationScoped
public class RecuperarEmpresaUseCase implements UseCase<RecuperarEmpresaCommand, EmpresaCriadaDto> {

    private final EmpresaRepository empresas;
    private final UsuarioEmpresaRepository usuarios;
    private final EmailRegistradoRepository emails;
    private final TokenSenhaRepository tokens;
    private final RefreshTokenRepository refreshTokens;
    private final NotificadorSenha notificador;
    private final AuditoriaAcesso auditoria;
    private final ContextoAutenticado contexto;
    private final ContextoRequisicao requisicao;
    private final Clock clock;

    public RecuperarEmpresaUseCase(EmpresaRepository empresas, UsuarioEmpresaRepository usuarios,
                                   EmailRegistradoRepository emails, TokenSenhaRepository tokens,
                                   RefreshTokenRepository refreshTokens, NotificadorSenha notificador,
                                   AuditoriaAcesso auditoria, ContextoAutenticado contexto,
                                   ContextoRequisicao requisicao, Clock clock) {
        this.empresas = empresas;
        this.usuarios = usuarios;
        this.emails = emails;
        this.tokens = tokens;
        this.refreshTokens = refreshTokens;
        this.notificador = notificador;
        this.auditoria = auditoria;
        this.contexto = contexto;
        this.requisicao = requisicao;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Result<EmpresaCriadaDto> execute(RecuperarEmpresaCommand cmd) {
        var nome = NomeUsuario.tryCreate(cmd.adminNome());
        var email = EmailUsuario.tryCreate(cmd.adminEmail());
        var telefone = TelefoneUsuario.tryCreateOpcional(cmd.adminTelefone());
        var validacao = Result.combineAll(nome, email, telefone);
        if (validacao.isFailure())
            return Result.propagate(validacao);

        var empresa = empresas.buscarPorIdBloqueando(cmd.empresaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException(ErroIdentidade.EMPRESA_NAO_ENCONTRADA.mensagem()));
        var admins = usuarios.bloquearAdminsDaEmpresa(empresa.getId());
        var recusa = PoliticaRecuperacaoEmpresa.avaliar(empresa, admins);
        if (recusa.isPresent()) {
            var r = recusa.get();
            throw r == PoliticaRecuperacaoEmpresa.Recusa.EMPRESA_SUSPENSA
                    ? new ConflitoException("EMPRESA_SUSPENSA", ErroIdentidade.EMPRESA_SUSPENSA.mensagem())
                    : new ConflitoException("EMPRESA_COM_ADMIN_ATIVO", ErroIdentidade.EMPRESA_COM_ADMIN_ATIVO.mensagem());
        }
        if (emails.emUso(email.getValue()))
            throw new ConflitoException("EMAIL_INDISPONIVEL", ErroIdentidade.EMAIL_INDISPONIVEL.mensagem());

        var agora = clock.instant();
        for (var admin : admins) {
            if (admin.status() == StatusUsuario.PENDING) {
                admin.desativar();
                usuarios.salvar(admin);
                tokens.invalidarPendentesDoUsuario(admin.getId(), agora);
                refreshTokens.revogarTodasDoTitular(TitularTipo.APP, admin.getId(), agora);
            }
        }
        var novo = UsuarioEmpresa.tryCreate(empresa.getId(), nome.getValue(), email.getValue(), telefone.getValue(),
                PapelUsuario.ADMIN).getOrThrow();
        usuarios.salvar(novo);
        notificador.notificar(novo, empresa.nome().valor(), TipoTokenSenha.SETUP);
        auditoria.registrar("EMPRESA_RECUPERADA", AuditoriaAcesso.SUCESSO, TitularTipo.PLATFORM, contexto.userId(),
                empresa.getId(), requisicao.ip(), null);
        return Result.ok(new EmpresaCriadaDto(empresa.getId(), empresa.nome().valor(), empresa.status().name(), novo.getId()));
    }
}
