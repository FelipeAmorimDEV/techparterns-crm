package tech.techparterns.funil.application.usecase;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import tech.techparterns.funil.application.command.CriarEtapaCommand;
import tech.techparterns.funil.domain.repository.EtapaRepository;
import tech.techparterns.shared.domain.CompanyId;
import tech.techparterns.shared.domain.Result;
import tech.techparterns.shared.domain.repository.UseCase;
import tech.techparterns.shared.infrastructure.security.TenantContext;

@ApplicationScoped
public class CriarEtapaUseCase implements UseCase<CriarEtapaCommand, UUID> {

    private final GarantirEtapasUseCase garantir;
    private final EtapaRepository etapas;
    private final TenantContext tenant;

    public CriarEtapaUseCase(GarantirEtapasUseCase garantir, EtapaRepository etapas, TenantContext tenant) {
        this.garantir = garantir;
        this.etapas = etapas;
        this.tenant = tenant;
    }

    @Override
    @Transactional
    public Result<UUID> execute(CriarEtapaCommand cmd) {
        var criado = garantir.execute(cmd);
        if (criado.isFailure())
            return Result.propagate(criado);
        var empresa = CompanyId.reconstituir(tenant.requireCompanyId());
        var id = etapas.listarPorEmpresa(empresa).stream()
                .filter(etapa -> etapa.ordem().valor() == cmd.ordem())
                .map(etapa -> etapa.getId())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Etapa criada não localizada pela ordem"));
        return Result.ok(id);
    }
}
