package tech.techparterns.identidade.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import tech.techparterns.identidade.application.command.UsuarioIdCommand;
import tech.techparterns.identidade.application.service.AuditoriaAcesso;
import tech.techparterns.identidade.application.service.NotificadorSenha;
import tech.techparterns.identidade.domain.model.*;
import tech.techparterns.identidade.domain.port.ContextoRequisicao;
import tech.techparterns.identidade.domain.repository.EmpresaRepository;
import tech.techparterns.identidade.domain.repository.UsuarioEmpresaRepository;
import tech.techparterns.identidade.infrastructure.security.ContextoAutenticado;
import tech.techparterns.shared.domain.Result;
import tech.techparterns.shared.domain.exception.RecursoNaoEncontradoException;
import tech.techparterns.shared.domain.repository.UseCase;

/** Reenvio manual do SETUP pelo admin (RF-IA-18): só usuário PENDING, no mínimo 60 s entre envios. */
@ApplicationScoped
public class ReenviarDefinicaoSenhaUseCase implements UseCase<UsuarioIdCommand, Void> {

    private final UsuarioEmpresaRepository usuarios;
    private final EmpresaRepository empresas;
    private final NotificadorSenha notificador;
    private final AuditoriaAcesso auditoria;
    private final ContextoAutenticado contexto;
    private final ContextoRequisicao requisicao;

    public ReenviarDefinicaoSenhaUseCase(UsuarioEmpresaRepository usuarios, EmpresaRepository empresas,
                                         NotificadorSenha notificador, AuditoriaAcesso auditoria,
                                         ContextoAutenticado contexto, ContextoRequisicao requisicao) {
        this.usuarios = usuarios;
        this.empresas = empresas;
        this.notificador = notificador;
        this.auditoria = auditoria;
        this.contexto = contexto;
        this.requisicao = requisicao;
    }

    @Override
    @Transactional
    public Result<Void> execute(UsuarioIdCommand cmd) {
        var empresaId = contexto.empresaId();
        var usuario = usuarios.buscarPorId(cmd.id(), empresaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(ErroIdentidade.USUARIO_NAO_ENCONTRADO.mensagem()));
        if (usuario.status() != StatusUsuario.PENDING)
            return Result.fail(ErroIdentidade.USUARIO_NAO_PENDENTE.mensagem());
        notificador.exigirIntervaloDeReenvio(usuario.getId());
        var empresa = empresas.buscarPorId(empresaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(ErroIdentidade.EMPRESA_NAO_ENCONTRADA.mensagem()));
        notificador.notificar(usuario, empresa.nome().valor(), TipoTokenSenha.SETUP);
        auditoria.registrar("REENVIO_SETUP", AuditoriaAcesso.SUCESSO, TitularTipo.APP, contexto.userId(), empresaId,
                requisicao.ip(), null);
        return Result.ok();
    }
}
