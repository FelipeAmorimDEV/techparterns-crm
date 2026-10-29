package tech.techparterns.funil.application.usecase;

import java.time.Clock;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.transaction.Transactional;

import tech.techparterns.funil.application.command.MoverLeadEtapaCommand;
import tech.techparterns.funil.domain.model.Etapa;
import tech.techparterns.funil.domain.model.HistoricoEtapa;
import tech.techparterns.funil.domain.model.LeadMudouEtapa;
import tech.techparterns.funil.domain.model.TipoEtapa;
import tech.techparterns.funil.domain.port.ConsultaLeadFunil;
import tech.techparterns.funil.domain.repository.EtapaRepository;
import tech.techparterns.funil.domain.repository.HistoricoEtapaRepository;
import tech.techparterns.funil.domain.repository.NegocioFunilRepository;
import tech.techparterns.funil.domain.service.EntradaEtapa;
import tech.techparterns.shared.domain.CompanyId;
import tech.techparterns.shared.domain.Result;
import tech.techparterns.shared.domain.exception.DomainException;
import tech.techparterns.shared.domain.repository.UseCase;
import tech.techparterns.shared.infrastructure.security.TenantContext;

@ApplicationScoped
public class MoverLeadEtapaUseCase implements UseCase<MoverLeadEtapaCommand, Void> {

    public static final String NAO_E_DONO = "Vendedor não é o dono";
    public static final String LEAD_NAO_ENCONTRADO = "Lead não encontrado";
    public static final String NEGOCIO_NAO_ENCONTRADO = "Negócio do funil não encontrado";
    public static final String DESTINO_INVALIDO = "Destino não é uma etapa ABERTO ativa";
    public static final String SITUACAO_FINAL = "Lead ganho ou perdido não muda de etapa";

    private final ConsultaLeadFunil consultaLead;
    private final EtapaRepository etapas;
    private final NegocioFunilRepository negocios;
    private final HistoricoEtapaRepository historicos;
    private final Event<LeadMudouEtapa> mudancas;
    private final TenantContext tenant;
    private final Clock clock;

    public MoverLeadEtapaUseCase(ConsultaLeadFunil consultaLead, EtapaRepository etapas,
                                 NegocioFunilRepository negocios, HistoricoEtapaRepository historicos,
                                 Event<LeadMudouEtapa> mudancas, TenantContext tenant, Clock clock) {
        this.consultaLead = consultaLead;
        this.etapas = etapas;
        this.negocios = negocios;
        this.historicos = historicos;
        this.mudancas = mudancas;
        this.tenant = tenant;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Result<Void> execute(MoverLeadEtapaCommand cmd) {
        var empresa = CompanyId.reconstituir(tenant.requireCompanyId());
        var fato = consultaLead.buscar(empresa, cmd.leadId())
                .orElseThrow(() -> new DomainException(LEAD_NAO_ENCONTRADO));
        if (fato.exclusao())
            throw new DomainException(LEAD_NAO_ENCONTRADO);
        if (!"ABERTA".equals(fato.situacao()))
            return Result.fail(SITUACAO_FINAL);
        if (!"ADMIN".equals(tenant.role())) {
            var usuario = UUID.fromString(tenant.requireUserId());
            if (fato.dono() == null || !fato.dono().equals(usuario))
                return Result.fail(NAO_E_DONO);
        }

        if (cmd.etapaId() == null)
            return Result.fail(DESTINO_INVALIDO);
        var destino = etapas.buscarPorId(cmd.etapaId(), empresa).orElse(null);
        if (destino == null || destino.tipo() != TipoEtapa.ABERTO || !destino.ativa())
            return Result.fail(DESTINO_INVALIDO);

        var negocio = negocios.buscar(cmd.leadId(), empresa)
                .orElseThrow(() -> new DomainException(NEGOCIO_NAO_ENCONTRADO));
        if (negocio.etapaId().equals(destino.getId()))
            return Result.ok();

        var entrada = EntradaEtapa.avaliar(
                destino.exigeQualificacao(), destino.exigeDemo(),
                negocio.decisor(), negocio.dorPrincipal(), negocio.usuariosPrevistos(),
                negocio.prazoDecisao(), negocio.orcamento(), negocio.dataDemo());
        if (entrada.isFailure())
            return Result.propagate(entrada);

        var agora = clock.instant();
        var usuario = UUID.fromString(tenant.requireUserId());
        negocios.salvar(negocio.moverPara(destino.getId(), agora));
        var historico = HistoricoEtapa.tryCreate(negocio.etapaId(), destino.getId(), usuario, agora);
        if (historico.isFailure())
            return Result.propagate(historico);
        historicos.salvar(historico.getValue());
        mudancas.fire(new LeadMudouEtapa());
        return Result.ok();
    }
}
