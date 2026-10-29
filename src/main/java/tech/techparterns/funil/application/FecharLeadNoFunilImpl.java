package tech.techparterns.funil.application;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.transaction.Transactional;

import tech.techparterns.distribuicao.domain.port.FecharLeadNoFunil;
import tech.techparterns.funil.application.usecase.MarcarGanhoUseCase;
import tech.techparterns.funil.application.usecase.MarcarPerdidoUseCase;
import tech.techparterns.funil.application.usecase.MoverLeadEtapaUseCase;
import tech.techparterns.funil.domain.model.Etapa;
import tech.techparterns.funil.domain.model.HistoricoEtapa;
import tech.techparterns.funil.domain.model.LeadGanho;
import tech.techparterns.funil.domain.model.LeadPerdido;
import tech.techparterns.funil.domain.model.TextoPlano;
import tech.techparterns.funil.domain.model.TipoEtapa;
import tech.techparterns.funil.domain.model.ValorComercial;
import tech.techparterns.funil.domain.port.AtualizarFatosLead;
import tech.techparterns.funil.domain.port.ConsultaLeadFunil;
import tech.techparterns.funil.domain.repository.EtapaRepository;
import tech.techparterns.funil.domain.repository.HistoricoEtapaRepository;
import tech.techparterns.funil.domain.repository.MotivoPerdaRepository;
import tech.techparterns.funil.domain.repository.NegocioFunilRepository;
import tech.techparterns.shared.domain.CompanyId;
import tech.techparterns.shared.domain.Result;
import tech.techparterns.shared.domain.exception.DomainException;
import tech.techparterns.shared.infrastructure.security.TenantContext;

@ApplicationScoped
public class FecharLeadNoFunilImpl implements FecharLeadNoFunil {

    private final ConsultaLeadFunil consultaLead;
    private final EtapaRepository etapas;
    private final MotivoPerdaRepository motivos;
    private final NegocioFunilRepository negocios;
    private final HistoricoEtapaRepository historicos;
    private final AtualizarFatosLead atualizarFatos;
    private final Event<LeadGanho> ganhos;
    private final Event<LeadPerdido> perdidos;
    private final TenantContext tenant;
    private final Clock clock;

    public FecharLeadNoFunilImpl(ConsultaLeadFunil consultaLead, EtapaRepository etapas, MotivoPerdaRepository motivos,
                                 NegocioFunilRepository negocios, HistoricoEtapaRepository historicos,
                                 AtualizarFatosLead atualizarFatos, Event<LeadGanho> ganhos,
                                 Event<LeadPerdido> perdidos, TenantContext tenant, Clock clock) {
        this.consultaLead = consultaLead;
        this.etapas = etapas;
        this.motivos = motivos;
        this.negocios = negocios;
        this.historicos = historicos;
        this.atualizarFatos = atualizarFatos;
        this.ganhos = ganhos;
        this.perdidos = perdidos;
        this.tenant = tenant;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Result<Void> fechar(UUID leadId, String situacao, boolean quente, String uf, String cidade,
                               String planoFechado, BigDecimal valorFechado, UUID motivoPerdaId, String observacao) {
        var empresa = CompanyId.reconstituir(tenant.requireCompanyId());
        var usuario = UUID.fromString(tenant.requireUserId());
        if ("GANHO".equals(situacao))
            return ganho(empresa, usuario, leadId, quente, uf, cidade, planoFechado, valorFechado);
        if ("PERDIDO".equals(situacao))
            return perdido(empresa, usuario, leadId, quente, uf, cidade, motivoPerdaId, observacao);
        return Result.fail("Situação inválida");
    }

    private Result<Void> ganho(CompanyId empresa, UUID usuario, UUID leadId, boolean quente, String uf, String cidade,
                               String planoFechado, BigDecimal valorFechado) {
        recusarLeadExcluido(empresa, leadId);
        var planoResult = TextoPlano.tryCreate(planoFechado);
        Result<ValorComercial> valorResult = valorFechado == null || valorFechado.signum() <= 0
                ? Result.fail(MarcarGanhoUseCase.VALOR_FECHADO_NAO_POSITIVO)
                : ValorComercial.tryCreate(valorFechado);
        var validacao = Result.combineAll(planoResult, valorResult);
        if (validacao.isFailure())
            return Result.propagate(validacao);

        var destino = unicaAtiva(etapas.listarPorEmpresa(empresa), TipoEtapa.GANHO);
        if (destino == null)
            return Result.fail(MarcarGanhoUseCase.ETAPA_FINAL_AUSENTE);

        var negocio = negocios.buscar(leadId, empresa)
                .orElseThrow(() -> new DomainException(MoverLeadEtapaUseCase.NEGOCIO_NAO_ENCONTRADO));
        var agora = clock.instant();
        var fechado = negocio.comGanho(destino.getId(), agora, planoResult.getValue(), valorResult.getValue());
        var historico = HistoricoEtapa.tryCreate(negocio.etapaId(), destino.getId(), usuario, agora);
        if (historico.isFailure())
            return Result.propagate(historico);

        var fatos = atualizarFatos.atualizar(empresa, leadId, "GANHO", quente, uf, cidade);
        if (fatos.isFailure())
            throw new DomainException(fatos.getErrors());
        negocios.salvar(fechado);
        historicos.salvar(historico.getValue());
        ganhos.fire(new LeadGanho());
        return Result.ok();
    }

    private Result<Void> perdido(CompanyId empresa, UUID usuario, UUID leadId, boolean quente, String uf, String cidade,
                                 UUID motivoPerdaId, String observacao) {
        recusarLeadExcluido(empresa, leadId);
        if (motivoPerdaId == null)
            return Result.fail(MarcarPerdidoUseCase.MOTIVO_INVALIDO);
        var motivo = motivos.buscarPorId(motivoPerdaId, empresa).orElse(null);
        if (motivo == null || !motivo.ativo())
            return Result.fail(MarcarPerdidoUseCase.MOTIVO_INVALIDO);

        var destino = unicaAtiva(etapas.listarPorEmpresa(empresa), TipoEtapa.PERDIDO);
        if (destino == null)
            return Result.fail(MarcarPerdidoUseCase.ETAPA_FINAL_AUSENTE);

        var negocio = negocios.buscar(leadId, empresa)
                .orElseThrow(() -> new DomainException(MoverLeadEtapaUseCase.NEGOCIO_NAO_ENCONTRADO));
        var agora = clock.instant();
        var fechado = negocio.comPerdido(destino.getId(), agora, motivo.getId(), observacao);
        var historico = HistoricoEtapa.tryCreate(negocio.etapaId(), destino.getId(), usuario, agora);
        if (historico.isFailure())
            return Result.propagate(historico);

        var fatos = atualizarFatos.atualizar(empresa, leadId, "PERDIDO", quente, uf, cidade);
        if (fatos.isFailure())
            throw new DomainException(fatos.getErrors());
        negocios.salvar(fechado);
        historicos.salvar(historico.getValue());
        perdidos.fire(new LeadPerdido());
        return Result.ok();
    }

    private void recusarLeadExcluido(CompanyId empresa, UUID leadId) {
        var fato = consultaLead.buscar(empresa, leadId)
                .orElseThrow(() -> new DomainException(MoverLeadEtapaUseCase.LEAD_NAO_ENCONTRADO));
        if (fato.exclusao())
            throw new DomainException(MoverLeadEtapaUseCase.LEAD_NAO_ENCONTRADO);
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
