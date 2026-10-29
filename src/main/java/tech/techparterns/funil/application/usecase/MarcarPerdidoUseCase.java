package tech.techparterns.funil.application.usecase;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.transaction.Transactional;

import tech.techparterns.funil.application.command.MarcarPerdidoCommand;
import tech.techparterns.funil.domain.model.Etapa;
import tech.techparterns.funil.domain.model.HistoricoEtapa;
import tech.techparterns.funil.domain.model.LeadPerdido;
import tech.techparterns.funil.domain.model.TipoEtapa;
import tech.techparterns.funil.domain.port.AtualizarFatosLead;
import tech.techparterns.funil.domain.port.ConsultaLeadFunil;
import tech.techparterns.funil.domain.repository.EtapaRepository;
import tech.techparterns.funil.domain.repository.HistoricoEtapaRepository;
import tech.techparterns.funil.domain.repository.MotivoPerdaRepository;
import tech.techparterns.funil.domain.repository.NegocioFunilRepository;
import tech.techparterns.shared.domain.CompanyId;
import tech.techparterns.shared.domain.Result;
import tech.techparterns.shared.domain.exception.DomainException;
import tech.techparterns.shared.domain.repository.UseCase;
import tech.techparterns.shared.infrastructure.security.TenantContext;

@ApplicationScoped
public class MarcarPerdidoUseCase implements UseCase<MarcarPerdidoCommand, Void> {

    public static final String SITUACAO_FINAL = "Situação final não muda";
    public static final String MOTIVO_INVALIDO = "Motivo de perda não está ativo nesta empresa";
    public static final String ETAPA_FINAL_AUSENTE = "Não há exatamente uma etapa PERDIDO ativa";

    private final ConsultaLeadFunil consultaLead;
    private final EtapaRepository etapas;
    private final MotivoPerdaRepository motivos;
    private final NegocioFunilRepository negocios;
    private final HistoricoEtapaRepository historicos;
    private final AtualizarFatosLead atualizarFatos;
    private final Event<LeadPerdido> perdidos;
    private final TenantContext tenant;
    private final Clock clock;

    public MarcarPerdidoUseCase(ConsultaLeadFunil consultaLead, EtapaRepository etapas,
                                MotivoPerdaRepository motivos, NegocioFunilRepository negocios,
                                HistoricoEtapaRepository historicos, AtualizarFatosLead atualizarFatos,
                                Event<LeadPerdido> perdidos, TenantContext tenant, Clock clock) {
        this.consultaLead = consultaLead;
        this.etapas = etapas;
        this.motivos = motivos;
        this.negocios = negocios;
        this.historicos = historicos;
        this.atualizarFatos = atualizarFatos;
        this.perdidos = perdidos;
        this.tenant = tenant;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Result<Void> execute(MarcarPerdidoCommand cmd) {
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

        if (cmd.motivoPerdaId() == null)
            return Result.fail(MOTIVO_INVALIDO);
        var motivo = motivos.buscarPorId(cmd.motivoPerdaId(), empresa).orElse(null);
        if (motivo == null || !motivo.ativo())
            return Result.fail(MOTIVO_INVALIDO);

        var destino = unicaAtiva(etapas.listarPorEmpresa(empresa), TipoEtapa.PERDIDO);
        if (destino == null)
            return Result.fail(ETAPA_FINAL_AUSENTE);

        var negocio = negocios.buscar(cmd.leadId(), empresa)
                .orElseThrow(() -> new DomainException(MoverLeadEtapaUseCase.NEGOCIO_NAO_ENCONTRADO));
        var agora = clock.instant();
        var fechado = negocio.comPerdido(destino.getId(), agora, motivo.getId(), cmd.observacao());
        var historico = HistoricoEtapa.tryCreate(negocio.etapaId(), destino.getId(), usuario, agora);
        if (historico.isFailure())
            return Result.propagate(historico);

        var fatos = atualizarFatos.atualizar(empresa, cmd.leadId(), "PERDIDO");
        if (fatos.isFailure())
            throw new DomainException(fatos.getErrors());
        negocios.salvar(fechado);
        historicos.salvar(historico.getValue());
        perdidos.fire(new LeadPerdido());
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
