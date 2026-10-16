package tech.techparterns.lead.application.usecase;

import java.time.Clock;
import java.time.OffsetDateTime;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import tech.techparterns.lead.application.command.ExcluirLeadCommand;
import tech.techparterns.lead.domain.repository.LeadRepository;
import tech.techparterns.shared.domain.CompanyId;
import tech.techparterns.shared.domain.Result;
import tech.techparterns.shared.domain.exception.DomainException;
import tech.techparterns.shared.domain.exception.ErroDominio;
import tech.techparterns.shared.domain.repository.UseCase;
import tech.techparterns.shared.infrastructure.security.TenantContext;

@ApplicationScoped
public class ExcluirLeadUseCase implements UseCase<ExcluirLeadCommand, Void> {

    private final LeadRepository repository;
    private final TenantContext tenant;
    private final Clock clock;

    public ExcluirLeadUseCase(LeadRepository repository, TenantContext tenant, Clock clock) {
        this.repository = repository;
        this.tenant = tenant;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Result<Void> execute(ExcluirLeadCommand cmd) {
        if (!ImportarLeadsUseCase.PAPEL_ADMIN.equals(tenant.role()))
            throw new DomainException(ErroDominio.OPERACAO_RESTRITA_AO_ADMIN.mensagem());
        var empresa = CompanyId.reconstituir(tenant.requireCompanyId());

        var lead = repository.buscarPorId(cmd.id(), empresa)
                .orElseThrow(() -> new DomainException(ErroDominio.LEAD_NAO_ENCONTRADO.mensagem()));
        if (lead.isDeleted())
            return Result.ok();

        lead.excluirLogicamente(OffsetDateTime.now(clock));
        repository.salvar(lead);
        return Result.ok();
    }
}
