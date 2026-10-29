package tech.techparterns.identidade.application.usecase;

import java.time.Duration;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import tech.techparterns.identidade.application.command.SolicitarRecuperacaoSenhaCommand;
import tech.techparterns.identidade.application.dto.RespostaDto;
import tech.techparterns.identidade.application.service.AuditoriaAcesso;
import tech.techparterns.identidade.application.service.NotificadorSenha;
import tech.techparterns.identidade.domain.model.EmailUsuario;
import tech.techparterns.identidade.domain.model.StatusUsuario;
import tech.techparterns.identidade.domain.model.TipoTokenSenha;
import tech.techparterns.identidade.domain.model.TitularTipo;
import tech.techparterns.identidade.domain.port.ContextoRequisicao;
import tech.techparterns.identidade.domain.port.LimitadorTaxa;
import tech.techparterns.identidade.domain.repository.EmpresaRepository;
import tech.techparterns.identidade.domain.repository.UsuarioEmpresaRepository;
import tech.techparterns.identidade.infrastructure.config.IdentidadeConfig;
import tech.techparterns.shared.domain.Result;
import tech.techparterns.shared.domain.exception.LimiteExcedidoException;
import tech.techparterns.shared.domain.repository.UseCase;

/** Resposta sempre igual; só cria RESET (e e-mail) para conta ACTIVE de empresa ativa (CA-16). */
@ApplicationScoped
public class SolicitarRecuperacaoSenhaUseCase implements UseCase<SolicitarRecuperacaoSenhaCommand, RespostaDto> {

    private final UsuarioEmpresaRepository usuarios;
    private final EmpresaRepository empresas;
    private final NotificadorSenha notificador;
    private final AuditoriaAcesso auditoria;
    private final LimitadorTaxa limitador;
    private final ContextoRequisicao requisicao;
    private final IdentidadeConfig config;

    public SolicitarRecuperacaoSenhaUseCase(UsuarioEmpresaRepository usuarios, EmpresaRepository empresas,
                                            NotificadorSenha notificador, AuditoriaAcesso auditoria,
                                            LimitadorTaxa limitador, ContextoRequisicao requisicao,
                                            IdentidadeConfig config) {
        this.usuarios = usuarios;
        this.empresas = empresas;
        this.notificador = notificador;
        this.auditoria = auditoria;
        this.limitador = limitador;
        this.requisicao = requisicao;
        this.config = config;
    }

    @Override
    @Transactional
    public Result<RespostaDto> execute(SolicitarRecuperacaoSenhaCommand cmd) {
        String ip = requisicao.ip();
        var taxa = limitador.consumir("recuperar:ip:" + ip, config.rate().recuperacaoPor15Min(), Duration.ofMinutes(15));
        if (!taxa.permitido())
            throw new LimiteExcedidoException("Muitas solicitações. Tente novamente mais tarde.", taxa.retryAfterSegundos());

        boolean enviado = false;
        var email = EmailUsuario.tryCreate(cmd == null ? null : cmd.email());
        if (email.isOk()) {
            var usuario = usuarios.buscarPorEmail(email.getValue()).orElse(null);
            if (usuario != null && usuario.status() == StatusUsuario.ACTIVE) {
                var empresa = empresas.buscarPorId(usuario.empresaId()).orElse(null);
                if (empresa != null && empresa.estaAtiva()) {
                    notificador.notificar(usuario, empresa.nome().valor(), TipoTokenSenha.RESET);
                    auditoria.registrar("RECUPERACAO_SENHA", AuditoriaAcesso.SUCESSO, TitularTipo.APP, usuario.getId(),
                            usuario.empresaId(), ip, "RESET gerado");
                    enviado = true;
                }
            }
        }
        if (!enviado)
            auditoria.registrar("RECUPERACAO_SENHA", "IGNORADA", null, null, null, ip, null);
        return Result.ok(RespostaDto.SOLICITACAO_RECEBIDA);
    }
}
