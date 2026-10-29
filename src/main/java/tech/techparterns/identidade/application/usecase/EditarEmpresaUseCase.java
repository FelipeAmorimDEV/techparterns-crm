package tech.techparterns.identidade.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import tech.techparterns.identidade.application.command.EditarEmpresaCommand;
import tech.techparterns.identidade.application.service.AuditoriaAcesso;
import tech.techparterns.identidade.domain.model.ErroIdentidade;
import tech.techparterns.identidade.domain.model.NomeEmpresa;
import tech.techparterns.identidade.domain.model.TitularTipo;
import tech.techparterns.identidade.domain.port.ContextoRequisicao;
import tech.techparterns.identidade.domain.repository.EmpresaRepository;
import tech.techparterns.identidade.infrastructure.security.ContextoAutenticado;
import tech.techparterns.shared.domain.Result;
import tech.techparterns.shared.domain.exception.RecursoNaoEncontradoException;
import tech.techparterns.shared.domain.repository.UseCase;

/** O nome é o único campo editável da empresa (RF-IA-13). */
@ApplicationScoped
public class EditarEmpresaUseCase implements UseCase<EditarEmpresaCommand, Void> {

    private final EmpresaRepository empresas;
    private final AuditoriaAcesso auditoria;
    private final ContextoAutenticado contexto;
    private final ContextoRequisicao requisicao;

    public EditarEmpresaUseCase(EmpresaRepository empresas, AuditoriaAcesso auditoria, ContextoAutenticado contexto,
                                ContextoRequisicao requisicao) {
        this.empresas = empresas;
        this.auditoria = auditoria;
        this.contexto = contexto;
        this.requisicao = requisicao;
    }

    @Override
    @Transactional
    public Result<Void> execute(EditarEmpresaCommand cmd) {
        var nome = NomeEmpresa.tryCreate(cmd.nome());
        if (nome.isFailure())
            return Result.propagate(nome);
        var empresa = empresas.buscarPorId(cmd.id())
                .orElseThrow(() -> new RecursoNaoEncontradoException(ErroIdentidade.EMPRESA_NAO_ENCONTRADA.mensagem()));
        empresa.renomear(nome.getValue());
        empresas.salvar(empresa);
        auditoria.registrar("EMPRESA_EDITADA", AuditoriaAcesso.SUCESSO, TitularTipo.PLATFORM, contexto.userId(),
                empresa.getId(), requisicao.ip(), null);
        return Result.ok();
    }
}
