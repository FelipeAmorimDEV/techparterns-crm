package tech.techparterns.funil.application.usecase;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import tech.techparterns.funil.application.command.AlterarEtapaCommand;
import tech.techparterns.funil.domain.model.Etapa;
import tech.techparterns.funil.domain.model.OrdemEtapa;
import tech.techparterns.funil.domain.repository.EtapaRepository;
import tech.techparterns.funil.domain.service.NascimentoEtapas;
import tech.techparterns.shared.domain.CompanyId;
import tech.techparterns.shared.domain.Result;
import tech.techparterns.shared.domain.exception.DomainException;
import tech.techparterns.shared.domain.repository.UseCase;
import tech.techparterns.shared.infrastructure.security.TenantContext;

@ApplicationScoped
public class AlterarEtapaUseCase implements UseCase<AlterarEtapaCommand, Void> {

    public static final String NAO_ENCONTRADA = "Etapa não encontrada";

    private final EtapaRepository etapas;
    private final TenantContext tenant;

    public AlterarEtapaUseCase(EtapaRepository etapas, TenantContext tenant) {
        this.etapas = etapas;
        this.tenant = tenant;
    }

    @Override
    @Transactional
    public Result<Void> execute(AlterarEtapaCommand cmd) {
        var empresa = CompanyId.reconstituir(tenant.requireCompanyId());
        var etapa = buscar(cmd == null ? null : cmd.id(), empresa);
        var ordem = OrdemEtapa.tryCreate(cmd == null ? null : cmd.ordem());
        if (ordem.isOk() && ordemOcupada(empresa, etapa.getId(), ordem.getValue()))
            return Result.fail(NascimentoEtapas.ORDEM_REPETIDA);
        var alteracao = etapa.alterar(
                cmd == null ? null : cmd.nome(),
                cmd == null ? null : cmd.ordem(),
                cmd == null ? null : cmd.probabilidade(),
                cmd == null ? null : cmd.cor(),
                cmd == null ? null : cmd.slaDias());
        if (alteracao.isFailure())
            return Result.propagate(alteracao);
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

    private boolean ordemOcupada(CompanyId empresa, UUID id, OrdemEtapa ordem) {
        for (Etapa outra : etapas.listarPorEmpresa(empresa)) {
            if (outra.getId().equals(id))
                continue;
            if (outra.ordem().equals(ordem))
                return true;
        }
        return false;
    }
}
