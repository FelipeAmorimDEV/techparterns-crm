package tech.techparterns.identidade.application.usecase;

import java.time.Clock;
import java.time.Duration;
import java.util.Locale;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import tech.techparterns.identidade.application.command.LoginCommand;
import tech.techparterns.identidade.application.service.AuditoriaAcesso;
import tech.techparterns.identidade.application.service.SessaoService;
import tech.techparterns.identidade.application.service.SessaoService.SessaoEmitida;
import tech.techparterns.identidade.domain.model.*;
import tech.techparterns.identidade.domain.port.*;
import tech.techparterns.identidade.domain.repository.*;
import tech.techparterns.identidade.domain.service.PoliticaBloqueioLogin;
import tech.techparterns.identidade.infrastructure.config.IdentidadeConfig;
import tech.techparterns.shared.domain.Result;
import tech.techparterns.shared.domain.exception.DomainException;
import tech.techparterns.shared.domain.exception.LimiteExcedidoException;
import tech.techparterns.shared.domain.exception.NaoAutenticadoException;
import tech.techparterns.shared.domain.repository.UseCase;

/**
 * Login unificado (app_user e platform_user). Ordem: rate limit por IP, bloqueios (401 sem incrementar),
 * verificação de senha com custo equivalente em qualquer caminho, falha incrementa contadores, sucesso abre sessão.
 */
@ApplicationScoped
public class LoginUseCase implements UseCase<LoginCommand, SessaoEmitida> {

    static final String LOGIN = "LOGIN";

    private final UsuarioEmpresaRepository usuarios;
    private final DonoPlataformaRepository donos;
    private final EmpresaRepository empresas;
    private final ControleLoginRepository controle;
    private final HasherSenha hasher;
    private final GeradorTokenOpaco gerador;
    private final SessaoService sessoes;
    private final AuditoriaAcesso auditoria;
    private final LimitadorTaxa limitador;
    private final ContextoRequisicao requisicao;
    private final Clock clock;
    private final IdentidadeConfig config;
    private final PoliticaBloqueioLogin politica;

    public LoginUseCase(UsuarioEmpresaRepository usuarios, DonoPlataformaRepository donos, EmpresaRepository empresas,
                        ControleLoginRepository controle, HasherSenha hasher, GeradorTokenOpaco gerador,
                        SessaoService sessoes, AuditoriaAcesso auditoria, LimitadorTaxa limitador,
                        ContextoRequisicao requisicao, Clock clock, IdentidadeConfig config) {
        this.usuarios = usuarios;
        this.donos = donos;
        this.empresas = empresas;
        this.controle = controle;
        this.hasher = hasher;
        this.gerador = gerador;
        this.sessoes = sessoes;
        this.auditoria = auditoria;
        this.limitador = limitador;
        this.requisicao = requisicao;
        this.clock = clock;
        this.config = config;
        this.politica = new PoliticaBloqueioLogin(config.login().parMaxFalhas(), config.login().emailMaxFalhas(),
                config.login().ipMaxFalhas(), config.login().janela(), config.login().bloqueio());
    }

    @Override
    @Transactional(dontRollbackOn = DomainException.class)
    public Result<SessaoEmitida> execute(LoginCommand cmd) {
        String ip = requisicao.ip();
        var taxa = limitador.consumir("login:ip:" + ip, config.rate().loginPorMinuto(), Duration.ofMinutes(1));
        if (!taxa.permitido())
            throw new LimiteExcedidoException("Muitas tentativas. Tente novamente mais tarde.", taxa.retryAfterSegundos());

        var agora = clock.instant();
        String emailTexto = cmd == null || cmd.email() == null ? "" : cmd.email().trim().toLowerCase(Locale.ROOT);
        String senha = cmd == null || cmd.senha() == null ? "" : cmd.senha();
        String emailHash = gerador.hashTexto(emailTexto);

        if (controle.estaBloqueado(emailHash, ip, agora)) {
            hasher.consumirTempoEquivalente(senha);
            auditoria.registrar(LOGIN, "BLOQUEADO", null, null, null, ip, null);
            throw naoAutenticado();
        }

        UsuarioEmpresa usuario = null;
        DonoPlataforma dono = null;
        var email = EmailUsuario.tryCreate(emailTexto);
        if (email.isOk() && !SenhaPlana.excedeLimite(senha)) {
            usuario = usuarios.buscarPorEmail(email.getValue()).orElse(null);
            if (usuario == null)
                dono = donos.buscarPorEmail(email.getValue()).orElse(null);
        }
        HashSenha hash = usuario != null ? usuario.hashSenha() : dono != null ? dono.hashSenha() : null;
        boolean senhaConfere;
        if (hash != null) {
            senhaConfere = hasher.confere(senha, hash);
        } else {
            hasher.consumirTempoEquivalente(senha);
            senhaConfere = false;
        }

        Empresa empresa = null;
        boolean acesso = false;
        if (senhaConfere && usuario != null && usuario.podeAutenticar()) {
            empresa = empresas.buscarPorId(usuario.empresaId()).orElse(null);
            acesso = empresa != null && empresa.estaAtiva();
        } else if (senhaConfere && dono != null) {
            acesso = dono.podeAutenticar();
        }

        if (!acesso) {
            boolean bloqueou = controle.registrarFalha(emailHash, ip, agora, politica);
            var tipo = usuario != null ? TitularTipo.APP : dono != null ? TitularTipo.PLATFORM : null;
            var id = usuario != null ? usuario.getId() : dono != null ? dono.getId() : null;
            var empresaId = usuario != null ? usuario.empresaId() : null;
            auditoria.registrar(LOGIN, AuditoriaAcesso.FALHA, tipo, id, empresaId, ip, null);
            if (bloqueou)
                auditoria.registrar("LOGIN_BLOQUEIO", "DISPARADO", tipo, id, empresaId, ip, null);
            throw naoAutenticado();
        }

        controle.zerarPar(emailHash, ip);
        SessaoEmitida sessao;
        if (usuario != null) {
            usuario.registrarLogin(agora);
            usuarios.salvar(usuario);
            sessao = sessoes.abrir(TitularTipo.APP, usuario.getId(), PapelAcesso.valueOf(usuario.papel().name()),
                    usuario.empresaId(), null);
            auditoria.registrar(LOGIN, AuditoriaAcesso.SUCESSO, TitularTipo.APP, usuario.getId(), usuario.empresaId(), ip, null);
        } else {
            dono.registrarLogin(agora);
            donos.salvar(dono);
            sessao = sessoes.abrir(TitularTipo.PLATFORM, dono.getId(), PapelAcesso.PLATFORM_OWNER, null, null);
            auditoria.registrar(LOGIN, AuditoriaAcesso.SUCESSO, TitularTipo.PLATFORM, dono.getId(), null, ip, null);
        }
        return Result.ok(sessao);
    }

    private static NaoAutenticadoException naoAutenticado() {
        return new NaoAutenticadoException(ErroIdentidade.CREDENCIAIS_INVALIDAS.mensagem());
    }
}
