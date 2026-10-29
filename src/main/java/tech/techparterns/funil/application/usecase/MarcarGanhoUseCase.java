package tech.techparterns.funil.application.usecase;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.transaction.Transactional;

import tech.techparterns.funil.application.command.MarcarGanhoCommand;
import tech.techparterns.funil.domain.model.Etapa;
import tech.techparterns.funil.domain.model.HistoricoEtapa;
import tech.techparterns.funil.domain.model.LeadGanho;
import tech.techparterns.funil.domain.model.TextoPlano;
import tech.techparterns.funil.domain.model.TipoEtapa;
import tech.techparterns.funil.domain.model.ValorComercial;
import tech.techparterns.funil.domain.port.AtualizarFatosLead;
import tech.techparterns.funil.domain.port.ConsultaLeadFunil;
import tech.techparterns.funil.domain.repository.EtapaRepository;
import tech.techparterns.funil.domain.repository.HistoricoEtapaRepository;
import tech.techparterns.funil.domain.repository.NegocioFunilRepository;
import tech.techparterns.shared.domain.CompanyId;
import tech.techparterns.shared.domain.Result;
import tech.techparterns.shared.domain.exception.DomainException;
import tech.techparterns.shared.domain.repository.UseCase;
import tech.techparterns.shared.infrastructure.security.TenantContext;

@ApplicationScoped
public class MarcarGanhoUseCase implements UseCase<MarcarGanhoCommand, Void> {

    public static final String SITUACAO_FINAL = "Situação final não muda";
    public static final String VALOR_FECHADO_NAO_POSITIVO = "Valor fechado deve ser maior que zero";
    public static final String ETAPA_FINAL_AUSENTE = "Não há exatamente uma etapa GANHO ativa";

    private final ConsultaLeadFunil consultaLead;
    private final EtapaRepository etapas;
    private final NegocioFunilRepository negocios;
    private final HistoricoEtapaRepository historicos;
    private final AtualizarFatosLead atualizarFatos;
    private final Event<LeadGanho> ganhos;
    private final TenantContext tenant;
    private final Clock clock;

    public MarcarGanhoUseCase(ConsultaLeadFunil consultaLead, EtapaRepository etapas,
                              NegocioFunilRepository negocios, HistoricoEtapaRepository historicos,
                              AtualizarFatosLead atualizarFatos, Event<LeadGanho> ganhos,
                              TenantContext tenant, Clock clock) {
        this.consultaLead = consultaLead;
        this.etapas = etapas;
        this.negocios = negocios;
        this.historicos = historicos;
        this.atualizarFatos = atualizarFatos;
        this.ganhos = ganhos;
        this.tenant = tenant;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Result<Void> execute(MarcarGanhoCommand cmd) {
        var empresa = CompanyId.reconstituir(tenant.requireCompanyId());
        var fato = consultaLead.buscar(empresa, cmd.leadId())
                .orElseThrow(() -> new DomainException(MoverLeadEtapaUseCase.LEAD_NAO_ENCONTRADO));
        if (fato.exclusao())
            throw new DomainException(MoverLeadEtapaUseCase.LEAD_NAO_ENCONTRADO);
        if (!"ABERTA".equals(fato.situacao()))
            return Result.fail(SITUACAO_FINAL);
        var usuario = UUID.fromString(tenant.requireUserId());
        if (!"ADMIN".equals(tenant.role())) {
            if (fato.dono() == null || !fato.dono().equals(usuario))
                return Result.fail(MoverLeadEtapaUseCase.NAO_E_DONO);
        }

        var planoResult = TextoPlano.tryCreate(cmd.planoFechado());
        Result<ValorComercial> valorResult = cmd.valorFechado() == null || cmd.valorFechado().signum() <= 0
                ? Result.fail(VALOR_FECHADO_NAO_POSITIVO)
                : ValorComercial.tryCreate(cmd.valorFechado());
        var validacao = Result.combineAll(planoResult, valorResult);
        if (validacao.isFailure())
            return Result.propagate(validacao);

        var destino = unicaAtiva(etapas.listarPorEmpresa(empresa), TipoEtapa.GANHO);
        if (destino == null)
            return Result.fail(ETAPA_FINAL_AUSENTE);

        var negocio = negocios.buscar(cmd.leadId(), empresa)
                .orElseThrow(() -> new DomainException(MoverLeadEtapaUseCase.NEGOCIO_NAO_ENCONTRADO));
        var agora = clock.instant();
        var fechado = negocio.comGanho(destino.getId(), agora, planoResult.getValue(), valorResult.getValue());
        var historico = HistoricoEtapa.tryCreate(negocio.etapaId(), destino.getId(), usuario, agora);
        if (historico.isFailure())
            return Result.propagate(historico);

        var fatos = atualizarFatos.atualizar(empresa, cmd.leadId(), "GANHO");
        if (fatos.isFailure())
            throw new DomainException(fatos.getErrors());
        negocios.salvar(fechado);
        historicos.salvar(historico.getValue());
        ganhos.fire(new LeadGanho());
        return Result.ok();
    }

    private static Etapa unicaAtiva(List<Etapa> daEmpresa, TipoEtapa tipo) {
        Etapa unica = null;
        for (Etapa etapa : daEmpresa) {
            if (etapa.tipo() != tipo || !etapa.ativa())
                continue;
            if (unica != null)
                return null;
            unica = etapa;
        }
        return unica;
    }
}
