package tech.techparterns.funil.application;

import java.time.Clock;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.transaction.Transactional;

import tech.techparterns.funil.application.usecase.GarantirEtapasUseCase;
import tech.techparterns.funil.domain.model.Etapa;
import tech.techparterns.funil.domain.model.HistoricoEtapa;
import tech.techparterns.funil.domain.model.LeadMudouEtapa;
import tech.techparterns.funil.domain.model.NegocioFunil;
import tech.techparterns.funil.domain.model.TipoEtapa;
import tech.techparterns.funil.domain.repository.EtapaRepository;
import tech.techparterns.funil.domain.repository.HistoricoEtapaRepository;
import tech.techparterns.funil.domain.repository.NegocioFunilRepository;
import tech.techparterns.lead.domain.model.Lead;
import tech.techparterns.lead.domain.port.EntrarLeadNoFunil;
import tech.techparterns.shared.domain.Result;
import tech.techparterns.shared.domain.exception.DomainException;

@ApplicationScoped
public class EntrarLeadNoFunilImpl implements EntrarLeadNoFunil {

    public static final String SEM_ABERTO_ATIVA = "Não há etapa ABERTO ativa";

    private final GarantirEtapasUseCase garantirEtapas;
    private final EtapaRepository etapas;
    private final NegocioFunilRepository negocios;
    private final HistoricoEtapaRepository historicos;
    private final Event<LeadMudouEtapa> eventos;
    private final Clock clock;

    public EntrarLeadNoFunilImpl(GarantirEtapasUseCase garantirEtapas, EtapaRepository etapas,
                                 NegocioFunilRepository negocios, HistoricoEtapaRepository historicos,
                                 Event<LeadMudouEtapa> eventos, Clock clock) {
        this.garantirEtapas = garantirEtapas;
        this.etapas = etapas;
        this.negocios = negocios;
        this.historicos = historicos;
        this.eventos = eventos;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void entrar(Lead lead) {
        var empresa = lead.companyId();
        var garantidas = garantirEtapas.garantirAusentes(empresa);
        if (garantidas.isFailure())
            throw new DomainException(garantidas.getErrors());

        var destino = abertoAtivaDeMenorOrdem(etapas.listarPorEmpresa(empresa));
        if (destino == null)
            throw new DomainException(SEM_ABERTO_ATIVA);

        var agora = clock.instant();
        var negocio = exigir(NegocioFunil.tryCreate(lead.getId(), destino.getId(), agora));
        negocios.salvar(negocio);
        var historico = exigir(HistoricoEtapa.tryCreate(null, destino.getId(), null, agora));
        historicos.salvar(historico);
        eventos.fire(new LeadMudouEtapa());
    }

    private static Etapa abertoAtivaDeMenorOrdem(List<Etapa> daEmpresa) {
        Etapa escolhida = null;
        for (Etapa etapa : daEmpresa) {
            if (etapa.tipo() != TipoEtapa.ABERTO || !etapa.ativa() || etapa.ordem() == null)
                continue;
            if (escolhida == null || etapa.ordem().valor() < escolhida.ordem().valor())
                escolhida = etapa;
        }
        return escolhida;
    }

    private static <T> T exigir(Result<T> resultado) {
        if (resultado.isFailure())
            throw new DomainException(resultado.getErrors());
        return resultado.getValue();
    }
}
