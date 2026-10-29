package tech.techparterns.funil.application.usecase;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import tech.techparterns.funil.application.command.ApagarEtapaCommand;
import tech.techparterns.funil.domain.model.Etapa;
import tech.techparterns.funil.domain.repository.EtapaRepository;
import tech.techparterns.funil.domain.repository.NegocioFunilRepository;
import tech.techparterns.shared.domain.CompanyId;
import tech.techparterns.shared.domain.Result;
import tech.techparterns.shared.domain.exception.DomainException;
import tech.techparterns.shared.domain.repository.UseCase;
import tech.techparterns.shared.infrastructure.security.TenantContext;

@ApplicationScoped
public class ApagarEtapaUseCase implements UseCase<ApagarEtapaCommand, Void> {

    public static final String NAO_ENCONTRADA = "Etapa não encontrada";
    public static final String COM_LEAD = "Etapa com lead não é apagada";

    private final EtapaRepository etapas;
    private final NegocioFunilRepository negocios;
    private final TenantContext tenant;
    private final Clock clock;

    public ApagarEtapaUseCase(EtapaRepository etapas, NegocioFunilRepository negocios, TenantContext tenant,
                              Clock clock) {
        this.etapas = etapas;
        this.negocios = negocios;
        this.tenant = tenant;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Result<Void> execute(ApagarEtapaCommand cmd) {
        var empresa = CompanyId.reconstituir(tenant.requireCompanyId());
        var etapa = buscar(cmd == null ? null : cmd.id(), empresa);
        if (negocios.existeNaEtapa(etapa.getId()))
            return Result.fail(COM_LEAD);
        etapa.setDeletedAt(OffsetDateTime.now(clock));
        etapas.salvar(etapa);
        return Result.ok();
    }

    private Etapa buscar(UUID id, CompanyId empresa) {
        var etapa = etapas.buscarPorId(id, empresa)
                .orElseThrow(() -> new DomainException(NAO_ENCONTRADA));
        if (etapa.isDeleted())
            throw new DomainException(NAO_ENCONTRADA);
        return etapa;
    }
}
