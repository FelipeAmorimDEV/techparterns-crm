package tech.techparterns.identidade.application.usecase;

import java.time.Clock;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import tech.techparterns.identidade.application.command.SuspenderEmpresaCommand;
import tech.techparterns.identidade.application.service.AuditoriaAcesso;
import tech.techparterns.identidade.domain.model.ErroIdentidade;
import tech.techparterns.identidade.domain.model.MotivoSuspensao;
import tech.techparterns.identidade.domain.model.TitularTipo;
import tech.techparterns.identidade.domain.port.ContextoRequisicao;
import tech.techparterns.identidade.domain.repository.EmpresaRepository;
import tech.techparterns.identidade.infrastructure.security.ContextoAutenticado;
import tech.techparterns.shared.domain.Result;
import tech.techparterns.shared.domain.exception.ConflitoException;
import tech.techparterns.shared.domain.exception.RecursoNaoEncontradoException;
import tech.techparterns.shared.domain.repository.UseCase;

/** Suspende só {@code company.status}; não toca em usuários, convites nem refresh em lote (RN-IA-13). */
@ApplicationScoped
public class SuspenderEmpresaUseCase implements UseCase<SuspenderEmpresaCommand, Void> {

    private final EmpresaRepository empresas;
    private final AuditoriaAcesso auditoria;
    private final ContextoAutenticado contexto;
    private final ContextoRequisicao requisicao;
    private final Clock clock;

    public SuspenderEmpresaUseCase(EmpresaRepository empresas, AuditoriaAcesso auditoria,
                                   ContextoAutenticado contexto, ContextoRequisicao requisicao, Clock clock) {
        this.empresas = empresas;
        this.auditoria = auditoria;
        this.contexto = contexto;
        this.requisicao = requisicao;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Result<Void> execute(SuspenderEmpresaCommand cmd) {
        var motivo = MotivoSuspensao.tryCreate(cmd.motivo());
        if (motivo.isFailure())
            return Result.propagate(motivo);
        var empresa = empresas.buscarPorId(cmd.id())
                .orElseThrow(() -> new RecursoNaoEncontradoException(ErroIdentidade.EMPRESA_NAO_ENCONTRADA.mensagem()));
        var resultado = empresa.suspender(motivo.getValue(), clock.instant());
        if (resultado.isFailure())
            throw new ConflitoException("ESTADO_EMPRESA_INVALIDO", ErroIdentidade.ESTADO_EMPRESA_INVALIDO.mensagem());
        empresas.salvar(empresa);
        auditoria.registrar("EMPRESA_SUSPENSA", AuditoriaAcesso.SUCESSO, TitularTipo.PLATFORM, contexto.userId(),
                empresa.getId(), requisicao.ip(), null);
        return Result.ok();
    }
}
