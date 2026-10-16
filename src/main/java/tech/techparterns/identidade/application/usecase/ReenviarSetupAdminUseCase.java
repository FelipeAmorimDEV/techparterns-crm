package tech.techparterns.identidade.application.usecase;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import tech.techparterns.identidade.application.command.ReenviarSetupAdminCommand;
import tech.techparterns.identidade.application.service.AuditoriaAcesso;
import tech.techparterns.identidade.application.service.NotificadorSenha;
import tech.techparterns.identidade.domain.model.*;
import tech.techparterns.identidade.domain.port.ContextoRequisicao;
import tech.techparterns.identidade.domain.repository.EmpresaRepository;
import tech.techparterns.identidade.domain.repository.UsuarioEmpresaRepository;
import tech.techparterns.identidade.infrastructure.security.ContextoAutenticado;
import tech.techparterns.shared.domain.Result;
import tech.techparterns.shared.domain.exception.ConflitoException;
import tech.techparterns.shared.domain.exception.RecursoNaoEncontradoException;
import tech.techparterns.shared.domain.repository.UseCase;

/** O dono reenvia o SETUP de um admin PENDING de empresa ACTIVE (RF-IA-15). Empresa suspensa → 409. */
@ApplicationScoped
public class ReenviarSetupAdminUseCase implements UseCase<ReenviarSetupAdminCommand, Void> {

    private final EmpresaRepository empresas;
    private final UsuarioEmpresaRepository usuarios;
    private final NotificadorSenha notificador;
    private final AuditoriaAcesso auditoria;
    private final ContextoAutenticado contexto;
    private final ContextoRequisicao requisicao;

    public ReenviarSetupAdminUseCase(EmpresaRepository empresas, UsuarioEmpresaRepository usuarios,
                                     NotificadorSenha notificador, AuditoriaAcesso auditoria,
                                     ContextoAutenticado contexto, ContextoRequisicao requisicao) {
        this.empresas = empresas;
        this.usuarios = usuarios;
        this.notificador = notificador;
        this.auditoria = auditoria;
        this.contexto = contexto;
        this.requisicao = requisicao;
    }

    @Override
    @Transactional
    public Result<Void> execute(ReenviarSetupAdminCommand cmd) {
        var empresa = empresas.buscarPorId(cmd.empresaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException(ErroIdentidade.EMPRESA_NAO_ENCONTRADA.mensagem()));
        if (!empresa.estaAtiva())
            throw new ConflitoException("EMPRESA_SUSPENSA", ErroIdentidade.EMPRESA_SUSPENSA.mensagem());

        UsuarioEmpresa admin;
        if (cmd.usuarioId() != null) {
            admin = usuarios.buscarPorId(cmd.usuarioId(), empresa.getId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException(ErroIdentidade.USUARIO_NAO_ENCONTRADO.mensagem()));
        } else {
            List<UsuarioEmpresa> pendentes = usuarios.bloquearAdminsDaEmpresa(empresa.getId()).stream()
                    .filter(u -> u.status() == StatusUsuario.PENDING).toList();
            if (pendentes.size() != 1)
                return Result.fail(ErroIdentidade.USUARIO_NAO_PENDENTE.mensagem());
            admin = pendentes.get(0);
        }
        if (admin.papel() != PapelUsuario.ADMIN || admin.status() != StatusUsuario.PENDING)
            return Result.fail(ErroIdentidade.USUARIO_NAO_PENDENTE.mensagem());
        notificador.exigirIntervaloDeReenvio(admin.getId());
        notificador.notificar(admin, empresa.nome().valor(), TipoTokenSenha.SETUP);
        auditoria.registrar("REENVIO_SETUP_ADMIN", AuditoriaAcesso.SUCESSO, TitularTipo.PLATFORM, contexto.userId(),
                empresa.getId(), requisicao.ip(), null);
        return Result.ok();
    }
}
