package tech.techparterns.identidade.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import tech.techparterns.identidade.application.command.AlterarPapelCommand;
import tech.techparterns.identidade.application.service.AuditoriaAcesso;
import tech.techparterns.identidade.domain.model.*;
import tech.techparterns.identidade.domain.port.ContextoRequisicao;
import tech.techparterns.identidade.domain.repository.UsuarioEmpresaRepository;
import tech.techparterns.identidade.domain.service.PoliticaUltimoAdmin;
import tech.techparterns.identidade.infrastructure.security.ContextoAutenticado;
import tech.techparterns.shared.domain.Result;
import tech.techparterns.shared.domain.exception.RecursoNaoEncontradoException;
import tech.techparterns.shared.domain.repository.UseCase;

/** Promove SELLER a ADMIN ou rebaixa ADMIN a SELLER, sujeito à RN-IA-10. O JWT em uso mantém o papel antigo. */
@ApplicationScoped
public class AlterarPapelUsuarioUseCase implements UseCase<AlterarPapelCommand, Void> {

    private final UsuarioEmpresaRepository usuarios;
    private final AuditoriaAcesso auditoria;
    private final ContextoAutenticado contexto;
    private final ContextoRequisicao requisicao;

    public AlterarPapelUsuarioUseCase(UsuarioEmpresaRepository usuarios, AuditoriaAcesso auditoria,
                                      ContextoAutenticado contexto, ContextoRequisicao requisicao) {
        this.usuarios = usuarios;
        this.auditoria = auditoria;
        this.contexto = contexto;
        this.requisicao = requisicao;
    }

    @Override
    @Transactional
    public Result<Void> execute(AlterarPapelCommand cmd) {
        var papel = PapelParser.parse(cmd.role(), null);
        if (papel.isFailure())
            return Result.propagate(papel);
        var empresaId = contexto.empresaId();
        var admins = usuarios.bloquearAdminsDaEmpresa(empresaId);
        var alvo = usuarios.buscarPorId(cmd.id(), empresaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(ErroIdentidade.USUARIO_NAO_ENCONTRADO.mensagem()));
        if (alvo.papel() == papel.getValue())
            return Result.ok();
        if (papel.getValue() == PapelUsuario.SELLER && !PoliticaUltimoAdmin.podeRemoverAdminAtivo(admins, alvo.getId()))
            return Result.fail(ErroIdentidade.ULTIMO_ADMIN.mensagem());
        alvo.alterarPapel(papel.getValue());
        usuarios.salvar(alvo);
        auditoria.registrar("USUARIO_PAPEL_ALTERADO", AuditoriaAcesso.SUCESSO, TitularTipo.APP, contexto.userId(),
                empresaId, requisicao.ip(), "novo=" + papel.getValue().name());
        return Result.ok();
    }
}
