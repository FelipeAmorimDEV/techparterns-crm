package tech.techparterns.identidade.application.usecase;

import java.time.Clock;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import tech.techparterns.identidade.application.command.DefinirSenhaCommand;
import tech.techparterns.identidade.application.service.AuditoriaAcesso;
import tech.techparterns.identidade.domain.model.*;
import tech.techparterns.identidade.domain.port.ContextoRequisicao;
import tech.techparterns.identidade.domain.port.GeradorTokenOpaco;
import tech.techparterns.identidade.domain.port.HasherSenha;
import tech.techparterns.identidade.domain.repository.*;
import tech.techparterns.shared.domain.Result;
import tech.techparterns.shared.domain.repository.UseCase;

/**
 * Conclui SETUP ou RESET: consome o token (uso único), grava a senha, ativa o usuário e revoga todas as sessões.
 * Não abre sessão (DEL-15b).
 */
@ApplicationScoped
public class DefinirSenhaPorTokenUseCase implements UseCase<DefinirSenhaCommand, Void> {

    private final TokenSenhaRepository tokens;
    private final UsuarioEmpresaRepository usuarios;
    private final EmpresaRepository empresas;
    private final RefreshTokenRepository refreshTokens;
    private final GeradorTokenOpaco gerador;
    private final HasherSenha hasher;
    private final AuditoriaAcesso auditoria;
    private final ContextoRequisicao requisicao;
    private final Clock clock;

    public DefinirSenhaPorTokenUseCase(TokenSenhaRepository tokens, UsuarioEmpresaRepository usuarios,
                                       EmpresaRepository empresas, RefreshTokenRepository refreshTokens,
                                       GeradorTokenOpaco gerador, HasherSenha hasher, AuditoriaAcesso auditoria,
                                       ContextoRequisicao requisicao, Clock clock) {
        this.tokens = tokens;
        this.usuarios = usuarios;
        this.empresas = empresas;
        this.refreshTokens = refreshTokens;
        this.gerador = gerador;
        this.hasher = hasher;
        this.auditoria = auditoria;
        this.requisicao = requisicao;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Result<Void> execute(DefinirSenhaCommand cmd) {
        var agora = clock.instant();
        if (cmd.token() == null || cmd.token().isBlank())
            return Result.fail(ErroIdentidade.TOKEN_SENHA_INVALIDO.mensagem());
        var token = tokens.buscarPorHash(gerador.hash(cmd.token())).orElse(null);
        if (token == null || !token.estaValido(agora))
            return Result.fail(ErroIdentidade.TOKEN_SENHA_INVALIDO.mensagem());

        var senha = SenhaPlana.tryCreate(cmd.senha());
        if (senha.isFailure())
            return Result.propagate(senha);

        var usuario = usuarios.buscarPorIdSemEmpresa(token.usuarioId()).orElse(null);
        var empresa = usuario == null ? null : empresas.buscarPorId(usuario.empresaId()).orElse(null);
        boolean estadoOk = usuario != null && empresa != null && empresa.estaAtiva()
                && (token.tipo() == TipoTokenSenha.SETUP
                        ? usuario.status() == StatusUsuario.PENDING
                        : usuario.status() == StatusUsuario.ACTIVE);
        if (!estadoOk || !tokens.consumirSePossivel(token.getId(), agora))
            return Result.fail(ErroIdentidade.TOKEN_SENHA_INVALIDO.mensagem());

        var resultado = usuario.definirSenha(hasher.hash(senha.getValue()));
        if (resultado.isFailure())
            return Result.propagate(resultado);
        usuarios.salvar(usuario);
        refreshTokens.revogarTodasDoTitular(TitularTipo.APP, usuario.getId(), agora);
        auditoria.registrar(token.tipo() == TipoTokenSenha.SETUP ? "SENHA_DEFINIDA" : "SENHA_REDEFINIDA",
                AuditoriaAcesso.SUCESSO, TitularTipo.APP, usuario.getId(), usuario.empresaId(), requisicao.ip(), null);
        return Result.ok();
    }
}
