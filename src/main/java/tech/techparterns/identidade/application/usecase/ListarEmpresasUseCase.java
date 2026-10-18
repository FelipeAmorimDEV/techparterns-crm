package tech.techparterns.identidade.application.usecase;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

import tech.techparterns.identidade.application.dto.EmpresaListadaDto;
import tech.techparterns.identidade.application.query.ListarEmpresasQuery;
import tech.techparterns.identidade.domain.repository.EmpresaRepository;
import tech.techparterns.identidade.domain.repository.EnvioEmailRepository;
import tech.techparterns.shared.domain.Result;
import tech.techparterns.shared.domain.repository.UseCase;

/** Leitura do dono (RF-IA-12): indicadores "sem admin ativo", "tem admin PENDING" e status do último envio. */
@ApplicationScoped
public class ListarEmpresasUseCase implements UseCase<ListarEmpresasQuery, List<EmpresaListadaDto>> {

    private final EmpresaRepository empresas;
    private final EnvioEmailRepository envios;

    public ListarEmpresasUseCase(EmpresaRepository empresas, EnvioEmailRepository envios) {
        this.empresas = empresas;
        this.envios = envios;
    }

    @Override
    public Result<List<EmpresaListadaDto>> execute(ListarEmpresasQuery query) {
        return Result.ok(empresas.listarResumos().stream().map(r -> {
            var e = r.empresa();
            String statusEnvio = r.temAdminPending()
                    ? envios.ultimoStatusDosAdminsPending(e.getId()).map(Enum::name).orElse(null)
                    : null;
            return new EmpresaListadaDto(e.getId(), e.nome().valor(), e.status().name(), e.suspensaEm(),
                    e.motivoSuspensao() == null ? null : e.motivoSuspensao().valor(), r.semAdminAtivo(),
                    r.temAdminPending(), statusEnvio);
        }).toList());
    }
}
