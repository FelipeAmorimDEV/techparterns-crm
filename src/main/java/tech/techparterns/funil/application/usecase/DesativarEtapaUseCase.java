package tech.techparterns.funil.application.usecase;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import tech.techparterns.funil.application.command.DesativarEtapaCommand;
import tech.techparterns.funil.domain.model.Etapa;
import tech.techparterns.funil.domain.model.TipoEtapa;
import tech.techparterns.funil.domain.repository.EtapaRepository;
import tech.techparterns.funil.domain.repository.NegocioFunilRepository;
import tech.techparterns.shared.domain.CompanyId;
import tech.techparterns.shared.domain.Result;
import tech.techparterns.shared.domain.exception.DomainException;
import tech.techparterns.shared.domain.repository.UseCase;
import tech.techparterns.shared.infrastructure.security.TenantContext;

@ApplicationScoped
public class DesativarEtapaUseCase implements UseCase<DesativarEtapaCommand, Void> {

    public static final String NAO_ENCONTRADA = "Etapa não encontrada";
    public static final String COM_LEAD = "Etapa com lead não é desativada";
    public static final String ULTIMA_ABERTA = "A última etapa ABERTO ativa não é desativada";
    public static final String UNICA_GANHO = "A única etapa GANHO ativa não é desativada";
    public static final String UNICA_PERDIDO = "A única etapa PERDIDO ativa não é desativada";

    private final EtapaRepository etapas;
    private final NegocioFunilRepository negocios;
    private final TenantContext tenant;

    public DesativarEtapaUseCase(EtapaRepository etapas, NegocioFunilRepository negocios, TenantContext tenant) {
        this.etapas = etapas;
        this.negocios = negocios;
        this.tenant = tenant;
    }

    @Override
    @Transactional
    public Result<Void> execute(DesativarEtapaCommand cmd) {
        var empresa = CompanyId.reconstituir(tenant.requireCompanyId());
        var etapa = buscar(cmd == null ? null : cmd.id(), empresa);
        if (negocios.existeNaEtapa(etapa.getId()))
            return Result.fail(COM_LEAD);
        if (etapa.tipo() == TipoEtapa.ABERTO && etapa.ativa() && !outraAbertaAtiva(empresa, etapa.getId()))
            return Result.fail(ULTIMA_ABERTA);
        if (etapa.tipo() == TipoEtapa.GANHO && etapa.ativa())
            return Result.fail(UNICA_GANHO);
        if (etapa.tipo() == TipoEtapa.PERDIDO && etapa.ativa())
            return Result.fail(UNICA_PERDIDO);
        etapa.desativar();
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

    private boolean outraAbertaAtiva(CompanyId empresa, UUID id) {
        for (Etapa outra : etapas.listarPorEmpresa(empresa)) {
            if (outra.getId().equals(id) || outra.isDeleted())
                continue;
            if (outra.tipo() == TipoEtapa.ABERTO && outra.ativa())
                return true;
        }
        return false;
    }
}
