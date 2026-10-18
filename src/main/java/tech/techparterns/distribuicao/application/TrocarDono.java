package tech.techparterns.distribuicao.application;

import java.time.Clock;
import java.util.Objects;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.transaction.Transactional;

import tech.techparterns.distribuicao.domain.model.Atribuicao;
import tech.techparterns.distribuicao.domain.model.DonoLead;
import tech.techparterns.distribuicao.domain.model.LeadAtribuido;
import tech.techparterns.distribuicao.domain.model.RegraAtribuicao;
import tech.techparterns.distribuicao.domain.repository.DistribuicaoRepository;

@ApplicationScoped
public class TrocarDono {

    private final DistribuicaoRepository repositorio;
    private final Event<LeadAtribuido> eventos;
    private final Clock clock;

    public TrocarDono(DistribuicaoRepository repositorio, Event<LeadAtribuido> eventos, Clock clock) {
        this.repositorio = repositorio;
        this.eventos = eventos;
        this.clock = clock;
    }

    /** @return true quando o dono mudou e a linha foi gravada */
    @Transactional
    public boolean trocar(String companyId, UUID leadId, UUID novoDono, RegraAtribuicao regra, String motivo, UUID autor) {
        var atual = repositorio.buscarDono(companyId, leadId).orElse(null);
        UUID donoAtual = atual == null ? null : atual.ownerId();
        if (Objects.equals(donoAtual, novoDono))
            return false;
        var agora = clock.instant();
        var id = UUID.randomUUID();
        var atribuidoEm = novoDono == null ? null : agora;
        repositorio.salvarDono(new DonoLead(leadId, companyId, novoDono, atribuidoEm, false), agora);
        repositorio.inserirAtribuicao(new Atribuicao(id, companyId, leadId, donoAtual, novoDono, regra, motivo, autor, agora));
        eventos.fire(new LeadAtribuido(companyId, leadId, donoAtual, novoDono, regra.name(), id));
        return true;
    }

    @Transactional
    public void garantirSemDono(String companyId, UUID leadId) {
        if (repositorio.buscarDono(companyId, leadId).isEmpty())
            repositorio.salvarDono(new DonoLead(leadId, companyId, null, null, false), clock.instant());
    }
}
