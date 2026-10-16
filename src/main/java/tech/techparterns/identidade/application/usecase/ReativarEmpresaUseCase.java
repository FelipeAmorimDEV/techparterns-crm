package tech.techparterns.identidade.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import tech.techparterns.identidade.application.command.EmpresaIdCommand;
import tech.techparterns.identidade.application.service.AuditoriaAcesso;
import tech.techparterns.identidade.domain.model.ErroIdentidade;
import tech.techparterns.identidade.domain.model.TitularTipo;
import tech.techparterns.identidade.domain.port.ContextoRequisicao;
import tech.techparterns.identidade.domain.repository.EmpresaRepository;
import tech.techparterns.identidade.infrastructure.security.ContextoAutenticado;
import tech.techparterns.shared.domain.Result;
import tech.techparterns.shared.domain.exception.ConflitoException;
import tech.techparterns.shared.domain.exception.RecursoNaoEncontradoException;
import tech.techparterns.shared.domain.repository.UseCase;

@ApplicationScoped
public class ReativarEmpresaUseCase implements UseCase<EmpresaIdCommand, Void> {

    private final EmpresaRepository empresas;
    private final AuditoriaAcesso auditoria;
    private final ContextoAutenticado contexto;
    private final ContextoRequisicao requisicao;

    public ReativarEmpresaUseCase(EmpresaRepository empresas, AuditoriaAcesso auditoria,
                                  ContextoAutenticado contexto, ContextoRequisicao requisicao) {
        this.empresas = empresas;
        this.auditoria = auditoria;
        this.contexto = contexto;
        this.requisicao = requisicao;
    }

    @Override
    @Transactional
    public Result<Void> execute(EmpresaIdCommand cmd) {
        var empresa = empresas.buscarPorId(cmd.id())
                .orElseThrow(() -> new RecursoNaoEncontradoException(ErroIdentidade.EMPRESA_NAO_ENCONTRADA.mensagem()));
        if (empresa.reativar().isFailure())
            throw new ConflitoException("ESTADO_EMPRESA_INVALIDO", ErroIdentidade.ESTADO_EMPRESA_INVALIDO.mensagem());
        empresas.salvar(empresa);
        auditoria.registrar("EMPRESA_REATIVADA", AuditoriaAcesso.SUCESSO, TitularTipo.PLATFORM, contexto.userId(),
                empresa.getId(), requisicao.ip(), null);
        return Result.ok();
    }
}
