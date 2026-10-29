package tech.techparterns.lead.infrastructure.persistence;

import java.time.Clock;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import tech.techparterns.funil.domain.port.AtualizarFatosLead;
import tech.techparterns.lead.domain.model.SituacaoLead;
import tech.techparterns.lead.domain.repository.LeadRepository;
import tech.techparterns.shared.domain.CompanyId;
import tech.techparterns.shared.domain.Result;
import tech.techparterns.shared.domain.exception.DomainException;

@ApplicationScoped
public class AtualizarFatosLeadImpl implements AtualizarFatosLead {

    static final String LEAD_NAO_ENCONTRADO = "Lead não encontrado";

    private final LeadRepository leads;
    private final Clock clock;

    public AtualizarFatosLeadImpl(LeadRepository leads, Clock clock) {
        this.leads = leads;
        this.clock = clock;
    }

    @Override
    public Result<Void> atualizar(CompanyId companyId, UUID leadId, String situacao) {
        var lead = leads.buscarPorId(leadId, companyId)
                .orElseThrow(() -> new DomainException(LEAD_NAO_ENCONTRADO));
        var novo = lead.comFatos(
                SituacaoLead.valueOf(situacao), lead.quente(), lead.uf(), lead.cidade(), clock.instant());
        if (novo.isFailure())
            return Result.propagate(novo);
        leads.salvar(novo.getValue());
        return Result.ok();
    }

    @Override
    public Result<Void> atualizar(CompanyId companyId, UUID leadId, String situacao,
                                  boolean quente, String uf, String cidade) {
        var lead = leads.buscarPorId(leadId, companyId)
                .orElseThrow(() -> new DomainException(LEAD_NAO_ENCONTRADO));
        var novo = lead.comFatos(SituacaoLead.valueOf(situacao), quente, uf, cidade, clock.instant());
        if (novo.isFailure())
            return Result.propagate(novo);
        leads.salvar(novo.getValue());
        return Result.ok();
    }
}
