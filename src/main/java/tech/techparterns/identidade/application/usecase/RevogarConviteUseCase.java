package tech.techparterns.identidade.application.usecase;

import java.time.Clock;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import tech.techparterns.identidade.application.command.ConviteIdCommand;
import tech.techparterns.identidade.application.service.AuditoriaAcesso;
import tech.techparterns.identidade.domain.model.ErroIdentidade;
import tech.techparterns.identidade.domain.model.TitularTipo;
import tech.techparterns.identidade.domain.port.ContextoRequisicao;
import tech.techparterns.identidade.domain.repository.ConviteRepository;
import tech.techparterns.identidade.infrastructure.security.ContextoAutenticado;
import tech.techparterns.shared.domain.Result;
import tech.techparterns.shared.domain.exception.RecursoNaoEncontradoException;
import tech.techparterns.shared.domain.repository.UseCase;

/** Idempotente (DTP-8a): revogar de novo não altera {@code revogado_em}. Convite de outra empresa é 404. */
@ApplicationScoped
public class RevogarConviteUseCase implements UseCase<ConviteIdCommand, Void> {

    private final ConviteRepository convites;
    private final AuditoriaAcesso auditoria;
    private final ContextoAutenticado contexto;
    private final ContextoRequisicao requisicao;
    private final Clock clock;

    public RevogarConviteUseCase(ConviteRepository convites, AuditoriaAcesso auditoria, ContextoAutenticado contexto,
                                 ContextoRequisicao requisicao, Clock clock) {
        this.convites = convites;
        this.auditoria = auditoria;
        this.contexto = contexto;
        this.requisicao = requisicao;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Result<Void> execute(ConviteIdCommand cmd) {
        var convite = convites.buscarPorId(cmd.id(), contexto.empresaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException(ErroIdentidade.CONVITE_NAO_ENCONTRADO.mensagem()));
        if (convite.revogadoEm() != null)
            return Result.ok();
        convite.revogar(clock.instant());
        convites.salvar(convite);
        auditoria.registrar("CONVITE_REVOGADO", AuditoriaAcesso.SUCESSO, TitularTipo.APP, contexto.userId(),
                convite.empresaId(), requisicao.ip(), null);
        return Result.ok();
    }
}
