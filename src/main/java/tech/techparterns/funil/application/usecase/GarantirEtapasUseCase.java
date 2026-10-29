package tech.techparterns.funil.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import tech.techparterns.funil.application.command.CriarEtapaCommand;
import tech.techparterns.funil.domain.model.CorEtapa;
import tech.techparterns.funil.domain.model.Etapa;
import tech.techparterns.funil.domain.model.OrdemEtapa;
import tech.techparterns.funil.domain.model.Probabilidade;
import tech.techparterns.funil.domain.model.SlaDias;
import tech.techparterns.funil.domain.model.TipoEtapa;
import tech.techparterns.funil.domain.repository.EtapaRepository;
import tech.techparterns.funil.domain.service.NascimentoEtapas;
import tech.techparterns.shared.domain.CompanyId;
import tech.techparterns.shared.domain.Result;
import tech.techparterns.shared.domain.repository.UseCase;
import tech.techparterns.shared.infrastructure.security.TenantContext;

@ApplicationScoped
public class GarantirEtapasUseCase implements UseCase<CriarEtapaCommand, Void> {

    private final EtapaRepository etapas;
    private final TenantContext tenant;

    public GarantirEtapasUseCase(EtapaRepository etapas, TenantContext tenant) {
        this.etapas = etapas;
        this.tenant = tenant;
    }

    @Override
    @Transactional
    public Result<Void> execute(CriarEtapaCommand cmd) {
        var empresa = CompanyId.reconstituir(tenant.requireCompanyId());
        var pedido = pedido(cmd);
        if (pedido.isFailure())
            return Result.propagate(pedido);
        return aplicar(empresa, pedido.getValue());
    }

    @Transactional
    public Result<Void> garantirAusentes(CompanyId companyId) {
        return aplicar(companyId, null);
    }

    private Result<Void> aplicar(CompanyId empresa, NascimentoEtapas.Pedido pedido) {
        var descrito = NascimentoEtapas.descrever(empresa, etapas.listarPorEmpresa(empresa), pedido);
        if (!descrito.ok())
            return Result.fail(descrito.erros());
        for (Etapa etapa : descrito.etapas())
            etapas.salvar(etapa);
        return Result.ok();
    }

    private static Result<NascimentoEtapas.Pedido> pedido(CriarEtapaCommand cmd) {
        var nome = nome(cmd.nome());
        var tipo = tipo(cmd.tipo());
        var ordem = OrdemEtapa.tryCreate(cmd.ordem());
        var probabilidade = cmd.probabilidade() == null
                ? Result.<Probabilidade>ok(null)
                : Probabilidade.tryCreate(cmd.probabilidade());
        var cor = cmd.cor() == null ? Result.<CorEtapa>ok(null) : CorEtapa.tryCreate(cmd.cor());
        var sla = SlaDias.tryCreate(cmd.slaDias());
        var validacao = Result.combineAll(nome, tipo, ordem, probabilidade, cor, sla);
        if (validacao.isFailure())
            return Result.propagate(validacao);
        return Result.ok(new NascimentoEtapas.Pedido(
                nome.getValue(),
                tipo.getValue(),
                ordem.getValue(),
                cmd.ativa(),
                probabilidade.getValue(),
                cor.getValue(),
                sla.getValue(),
                cmd.exigeQualificacao(),
                cmd.exigeDemo()));
    }

    private static Result<String> nome(String nome) {
        if (nome == null || nome.isBlank())
            return Result.fail(Etapa.NOME_OBRIGATORIO);
        return Result.ok(nome.trim());
    }

    private static Result<TipoEtapa> tipo(String tipo) {
        if (tipo == null || tipo.isBlank())
            return Result.fail(Etapa.TIPO_OBRIGATORIO);
        try {
            return Result.ok(TipoEtapa.valueOf(tipo));
        } catch (IllegalArgumentException ex) {
            return Result.fail(Etapa.TIPO_OBRIGATORIO);
        }
    }
}
