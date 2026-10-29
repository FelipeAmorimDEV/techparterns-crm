package tech.techparterns.funil.application.usecase;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import tech.techparterns.funil.application.command.AtualizarDadosAbertosCommand;
import tech.techparterns.funil.application.dto.DadosAbertosDto;
import tech.techparterns.funil.domain.model.NegocioFunil;
import tech.techparterns.funil.domain.port.ConsultaLeadFunil;
import tech.techparterns.funil.domain.repository.NegocioFunilRepository;
import tech.techparterns.shared.domain.CompanyId;
import tech.techparterns.shared.domain.Result;
import tech.techparterns.shared.domain.exception.DomainException;
import tech.techparterns.shared.domain.repository.UseCase;
import tech.techparterns.shared.infrastructure.security.TenantContext;

@ApplicationScoped
public class AtualizarDadosAbertosUseCase implements UseCase<AtualizarDadosAbertosCommand, DadosAbertosDto> {

    public static final String NAO_E_DONO = "Vendedor não é o dono";
    public static final String LEAD_NAO_ENCONTRADO = "Lead não encontrado";
    public static final String NEGOCIO_NAO_ENCONTRADO = "Negócio do funil não encontrado";
    public static final String PREVISAO_INVALIDA = "Previsão de fechamento não é uma data válida";

    private final NegocioFunilRepository negocios;
    private final ConsultaLeadFunil consultaLead;
    private final TenantContext tenant;

    public AtualizarDadosAbertosUseCase(NegocioFunilRepository negocios, ConsultaLeadFunil consultaLead,
                                        TenantContext tenant) {
        this.negocios = negocios;
        this.consultaLead = consultaLead;
        this.tenant = tenant;
    }

    @Override
    @Transactional
    public Result<DadosAbertosDto> execute(AtualizarDadosAbertosCommand cmd) {
        var empresa = CompanyId.reconstituir(tenant.requireCompanyId());
        var previsao = previsao(cmd.previsaoFechamento());
        if (previsao.isFailure())
            return Result.propagate(previsao);

        var fato = consultaLead.buscar(empresa, cmd.leadId())
                .orElseThrow(() -> new DomainException(LEAD_NAO_ENCONTRADO));
        if (fato.exclusao())
            throw new DomainException(LEAD_NAO_ENCONTRADO);
        if (!"ADMIN".equals(tenant.role())) {
            var usuario = UUID.fromString(tenant.requireUserId());
            if (fato.dono() == null || !fato.dono().equals(usuario))
                return Result.fail(NAO_E_DONO);
        }

        var negocio = negocios.buscar(cmd.leadId(), empresa)
                .orElseThrow(() -> new DomainException(NEGOCIO_NAO_ENCONTRADO));
        var atualizado = negocio.comDadosAbertos(
                cmd.decisor(), cmd.dorPrincipal(), cmd.usuariosPrevistos(), cmd.prazoDecisao(), cmd.orcamento(),
                cmd.dataDemo(), cmd.valorEstimado(), cmd.planoInteresse(), previsao.getValue());
        if (atualizado.isFailure())
            return Result.propagate(atualizado);
        var gravado = atualizado.getValue();
        negocios.salvar(gravado);
        return Result.ok(paraDto(gravado));
    }

    private static Result<LocalDate> previsao(String texto) {
        if (texto == null || texto.isBlank())
            return Result.ok(null);
        try {
            return Result.ok(LocalDate.parse(texto));
        } catch (DateTimeParseException ex) {
            return Result.fail(PREVISAO_INVALIDA);
        }
    }

    private static DadosAbertosDto paraDto(NegocioFunil negocio) {
        return new DadosAbertosDto(
                negocio.etapaId(),
                negocio.decisor(),
                negocio.dorPrincipal(),
                negocio.usuariosPrevistos(),
                negocio.prazoDecisao(),
                negocio.orcamento(),
                negocio.dataDemo(),
                negocio.valorEstimado() == null ? null : negocio.valorEstimado().valor(),
                negocio.planoInteresse() == null ? null : negocio.planoInteresse().valor(),
                negocio.previsaoFechamento());
    }
}
