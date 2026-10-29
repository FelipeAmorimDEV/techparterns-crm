package tech.techparterns.funil.application.usecase;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

import tech.techparterns.funil.application.dto.MotivoPerdaItemDto;
import tech.techparterns.funil.application.query.ListarMotivosPerdaQuery;
import tech.techparterns.funil.domain.repository.MotivoPerdaRepository;
import tech.techparterns.shared.domain.CompanyId;
import tech.techparterns.shared.domain.Result;
import tech.techparterns.shared.domain.repository.UseCase;
import tech.techparterns.shared.infrastructure.security.TenantContext;

@ApplicationScoped
public class ListarMotivosPerdaUseCase implements UseCase<ListarMotivosPerdaQuery, List<MotivoPerdaItemDto>> {

    private final MotivoPerdaRepository motivos;
    private final TenantContext tenant;

    public ListarMotivosPerdaUseCase(MotivoPerdaRepository motivos, TenantContext tenant) {
        this.motivos = motivos;
        this.tenant = tenant;
    }

    @Override
    public Result<List<MotivoPerdaItemDto>> execute(ListarMotivosPerdaQuery query) {
        var empresa = CompanyId.reconstituir(tenant.requireCompanyId());
        var itens = motivos.listarPorEmpresa(empresa).stream()
                .map(motivo -> new MotivoPerdaItemDto(motivo.getId(), motivo.nome(), motivo.ativo()))
                .toList();
        return Result.ok(itens);
    }
}
