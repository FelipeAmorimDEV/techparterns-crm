package tech.techparterns.funil.application.usecase;

import java.time.Clock;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.inject.Instance;
import jakarta.transaction.Transactional;

import org.jboss.logging.Logger;

import tech.techparterns.funil.application.command.ColocarLeadsJaGravadosCommand;
import tech.techparterns.funil.domain.model.Etapa;
import tech.techparterns.funil.domain.model.HistoricoEtapa;
import tech.techparterns.funil.domain.model.LeadMudouEtapa;
import tech.techparterns.funil.domain.model.NegocioFunil;
import tech.techparterns.funil.domain.model.TipoEtapa;
import tech.techparterns.funil.domain.port.LeadsSemLinhaFunil;
import tech.techparterns.funil.domain.port.LeadsSemLinhaFunil.LeadSemLinha;
import tech.techparterns.funil.domain.repository.EtapaRepository;
import tech.techparterns.funil.domain.repository.HistoricoEtapaRepository;
import tech.techparterns.funil.domain.repository.NegocioFunilRepository;
import tech.techparterns.shared.domain.CompanyId;
import tech.techparterns.shared.domain.Result;
import tech.techparterns.shared.domain.exception.DomainException;
import tech.techparterns.shared.domain.repository.UseCase;

@ApplicationScoped
public class ColocarLeadsJaGravadosUseCase implements UseCase<ColocarLeadsJaGravadosCommand, Void> {

    public static final String SEM_ABERTO_ATIVA = "Não há etapa ABERTO ativa";
    public static final String SEM_GANHO_ATIVA = "Não há etapa GANHO ativa";
    public static final String SEM_PERDIDO_ATIVA = "Não há etapa PERDIDO ativa";
    public static final String SITUACAO_NAO_COLOCADA = "Situação do lead não admite colocação no funil";

    private static final Logger LOG = Logger.getLogger(ColocarLeadsJaGravadosUseCase.class);

    private final LeadsSemLinhaFunil leadsSemLinha;
    private final Instance<ColocarLeadsJaGravadosUseCase> proxy;
    private final GarantirEtapasUseCase garantirEtapas;
    private final EtapaRepository etapas;
    private final NegocioFunilRepository negocios;
    private final HistoricoEtapaRepository historicos;
    private final Event<LeadMudouEtapa> mudancas;
    private final Clock clock;

    public ColocarLeadsJaGravadosUseCase(LeadsSemLinhaFunil leadsSemLinha,
                                         Instance<ColocarLeadsJaGravadosUseCase> proxy,
                                         GarantirEtapasUseCase garantirEtapas,
                                         EtapaRepository etapas,
                                         NegocioFunilRepository negocios,
                                         HistoricoEtapaRepository historicos,
                                         Event<LeadMudouEtapa> mudancas,
                                         Clock clock) {
        this.leadsSemLinha = leadsSemLinha;
        this.proxy = proxy;
        this.garantirEtapas = garantirEtapas;
        this.etapas = etapas;
        this.negocios = negocios;
        this.historicos = historicos;
        this.mudancas = mudancas;
        this.clock = clock;
    }

    /** Sem @Transactional: cada lead abre a própria transação pelo proxy. */
    @Override
    public Result<Void> execute(ColocarLeadsJaGravadosCommand cmd) {
        for (LeadSemLinha lead : leadsSemLinha.listar()) {
            try {
                proxy.get().colocar(lead);
            } catch (Exception e) {
                LOG.errorf("Falha ao colocar lead já gravado no funil: %s", e.getClass().getSimpleName());
            }
        }
        return Result.ok();
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void colocar(LeadSemLinha lead) {
        var empresa = CompanyId.tryCreate(lead.companyId());
        if (empresa.isFailure())
            throw new DomainException(empresa.getErrors());
        var companyId = empresa.getValue();
        if (negocios.buscar(lead.leadId(), companyId).isPresent())
            return;

        var tipo = tipoDestino(lead.situacao());
        var garantidas = garantirEtapas.garantirAusentes(companyId);
        if (garantidas.isFailure())
            throw new DomainException(garantidas.getErrors());

        if (negocios.buscar(lead.leadId(), companyId).isPresent())
            return;

        var destino = escolher(tipo, etapas.listarPorEmpresa(companyId));
        if (destino == null)
            throw new DomainException(semEtapa(tipo));

        var agora = clock.instant();
        var negocio = exigir(NegocioFunil.tryCreate(lead.leadId(), destino.getId(), agora));
        negocios.salvar(negocio);
        var historico = exigir(HistoricoEtapa.tryCreate(null, destino.getId(), null, agora));
        historicos.salvar(historico);
        if (tipo == TipoEtapa.ABERTO)
            mudancas.fire(new LeadMudouEtapa());
    }

    private static TipoEtapa tipoDestino(String situacao) {
        if ("ABERTA".equals(situacao))
            return TipoEtapa.ABERTO;
        if ("GANHO".equals(situacao))
            return TipoEtapa.GANHO;
        if ("PERDIDO".equals(situacao))
            return TipoEtapa.PERDIDO;
        throw new DomainException(SITUACAO_NAO_COLOCADA);
    }

    private static Etapa escolher(TipoEtapa tipo, List<Etapa> daEmpresa) {
        if (tipo == TipoEtapa.ABERTO)
            return abertoAtivaDeMenorOrdem(daEmpresa);
        for (Etapa etapa : daEmpresa) {
            if (etapa.tipo() == tipo && etapa.ativa())
                return etapa;
        }
        return null;
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

    private static String semEtapa(TipoEtapa tipo) {
        return switch (tipo) {
            case ABERTO -> SEM_ABERTO_ATIVA;
            case GANHO -> SEM_GANHO_ATIVA;
            case PERDIDO -> SEM_PERDIDO_ATIVA;
        };
    }

    private static <T> T exigir(Result<T> resultado) {
        if (resultado.isFailure())
            throw new DomainException(resultado.getErrors());
        return resultado.getValue();
    }
}
