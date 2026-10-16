package tech.techparterns.identidade.application.usecase;

import java.time.Clock;
import java.time.Duration;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import tech.techparterns.identidade.application.command.RenovarSessaoCommand;
import tech.techparterns.identidade.application.service.AuditoriaAcesso;
import tech.techparterns.identidade.application.service.SessaoService;
import tech.techparterns.identidade.application.service.SessaoService.SessaoEmitida;
import tech.techparterns.identidade.domain.model.*;
import tech.techparterns.identidade.domain.port.*;
import tech.techparterns.identidade.domain.repository.*;
import tech.techparterns.identidade.domain.service.PoliticaReusoRefresh;
import tech.techparterns.identidade.infrastructure.config.IdentidadeConfig;
import tech.techparterns.shared.domain.Result;
import tech.techparterns.shared.domain.exception.DomainException;
import tech.techparterns.shared.domain.exception.LimiteExcedidoException;
import tech.techparterns.shared.domain.exception.NaoAutenticadoException;
import tech.techparterns.shared.domain.repository.UseCase;

/** Rotaciona o refresh dentro da mesma família; reuso fora da tolerância revoga a família inteira (RN-IA-16). */
@ApplicationScoped
public class RenovarSessaoUseCase implements UseCase<RenovarSessaoCommand, SessaoEmitida> {

    private static final String MENSAGEM = "Sessão inválida ou expirada.";

    private final RefreshTokenRepository refreshTokens;
    private final UsuarioEmpresaRepository usuarios;
    private final DonoPlataformaRepository donos;
    private final EmpresaRepository empresas;
    private final GeradorTokenOpaco gerador;
    private final SessaoService sessoes;
    private final AuditoriaAcesso auditoria;
    private final LimitadorTaxa limitador;
    private final ContextoRequisicao requisicao;
    private final Clock clock;
    private final IdentidadeConfig config;
    private final PoliticaReusoRefresh politicaReuso;

    public RenovarSessaoUseCase(RefreshTokenRepository refreshTokens, UsuarioEmpresaRepository usuarios,
                                DonoPlataformaRepository donos, EmpresaRepository empresas, GeradorTokenOpaco gerador,
                                SessaoService sessoes, AuditoriaAcesso auditoria, LimitadorTaxa limitador,
                                ContextoRequisicao requisicao, Clock clock, IdentidadeConfig config) {
        this.refreshTokens = refreshTokens;
        this.usuarios = usuarios;
        this.donos = donos;
        this.empresas = empresas;
        this.gerador = gerador;
        this.sessoes = sessoes;
        this.auditoria = auditoria;
        this.limitador = limitador;
        this.requisicao = requisicao;
        this.clock = clock;
        this.config = config;
        this.politicaReuso = new PoliticaReusoRefresh(config.refresh().toleranciaReuso());
    }

    @Override
    @Transactional(dontRollbackOn = DomainException.class)
    public Result<SessaoEmitida> execute(RenovarSessaoCommand cmd) {
        String ip = requisicao.ip();
        var taxa = limitador.consumir("refresh:ip:" + ip, config.rate().refreshPorMinuto(), Duration.ofMinutes(1));
        if (!taxa.permitido())
            throw new LimiteExcedidoException("Muitas requisições. Tente novamente mais tarde.", taxa.retryAfterSegundos());

        var agora = clock.instant();
        if (cmd.refreshToken() == null || cmd.refreshToken().isBlank())
            throw new NaoAutenticadoException(MENSAGEM);
        var token = refreshTokens.buscarPorHash(gerador.hash(cmd.refreshToken()))
                .orElseThrow(() -> new NaoAutenticadoException(MENSAGEM));

        if (token.foiRevogado() || token.expirado(agora))
            throw new NaoAutenticadoException(MENSAGEM);

        if (token.foiRotacionado()) {
            reuso(token, agora, ip);
            throw new NaoAutenticadoException(MENSAGEM);
        }
        if (!refreshTokens.rotacionarSePossivel(token.getId(), agora)) {
            // perdeu a corrida contra outra renovação do mesmo token: tratado como reuso tolerado
            throw new NaoAutenticadoException(MENSAGEM);
        }

        PapelAcesso papel;
        UUID empresaId = null;
        if (token.titularTipo() == TitularTipo.APP) {
            var usuario = usuarios.buscarPorIdSemEmpresa(token.titularId()).orElse(null);
            var empresa = usuario == null ? null : empresas.buscarPorId(usuario.empresaId()).orElse(null);
            if (usuario == null || !usuario.podeAutenticar() || empresa == null || !empresa.estaAtiva())
                throw new NaoAutenticadoException(MENSAGEM);
            papel = PapelAcesso.valueOf(usuario.papel().name());
            empresaId = usuario.empresaId();
        } else {
            var dono = donos.buscarPorId(token.titularId()).orElse(null);
            if (dono == null || !dono.podeAutenticar())
                throw new NaoAutenticadoException(MENSAGEM);
            papel = PapelAcesso.PLATFORM_OWNER;
        }
        var sessao = sessoes.abrir(token.titularTipo(), token.titularId(), papel, empresaId, token.familiaId());
        auditoria.registrar("REFRESH", AuditoriaAcesso.SUCESSO, token.titularTipo(), token.titularId(), empresaId, ip, null);
        return Result.ok(sessao);
    }

    private void reuso(RefreshToken token, java.time.Instant agora, String ip) {
        var decisao = politicaReuso.avaliar(token.rotacionadoEm(), agora);
        if (decisao == PoliticaReusoRefresh.Decisao.REUSO_REVOGAR_FAMILIA) {
            refreshTokens.revogarFamilia(token.familiaId(), agora);
            auditoria.registrar("REFRESH", "REUSO_FAMILIA_REVOGADA", token.titularTipo(), token.titularId(), null, ip, null);
        } else {
            auditoria.registrar("REFRESH", "REUSO_TOLERADO", token.titularTipo(), token.titularId(), null, ip, null);
        }
    }
}
