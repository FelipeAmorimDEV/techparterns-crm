package tech.techparterns.funil.application.usecase;

import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import tech.techparterns.funil.application.dto.QuadroFunilDto;
import tech.techparterns.funil.application.query.QuadroFunilQuery;
import tech.techparterns.funil.domain.model.Etapa;
import tech.techparterns.funil.domain.model.NegocioFunil;
import tech.techparterns.funil.domain.model.ValorComercial;
import tech.techparterns.funil.domain.port.ConsultaLeadFunil;
import tech.techparterns.funil.domain.repository.EtapaRepository;
import tech.techparterns.funil.domain.repository.NegocioFunilRepository;
import tech.techparterns.funil.domain.service.AtrasoEtapa;
import tech.techparterns.funil.domain.service.ValorColuna;
import tech.techparterns.shared.domain.CompanyId;
import tech.techparterns.shared.domain.Result;
import tech.techparterns.shared.domain.repository.UseCase;
import tech.techparterns.shared.infrastructure.security.TenantContext;

@ApplicationScoped
public class QuadroFunilUseCase implements UseCase<QuadroFunilQuery, QuadroFunilDto> {

    private final EtapaRepository etapas;
    private final NegocioFunilRepository negocios;
    private final ConsultaLeadFunil consultaLead;
    private final TenantContext tenant;
    private final Clock clock;

    public QuadroFunilUseCase(EtapaRepository etapas, NegocioFunilRepository negocios,
                              ConsultaLeadFunil consultaLead, TenantContext tenant, Clock clock) {
        this.etapas = etapas;
        this.negocios = negocios;
        this.consultaLead = consultaLead;
        this.tenant = tenant;
        this.clock = clock;
    }

    @Override
    public Result<QuadroFunilDto> execute(QuadroFunilQuery query) {
        var empresa = CompanyId.reconstituir(tenant.requireCompanyId());
        var filtros = query == null ? new QuadroFunilQuery(null, null) : query;
        boolean admin = "ADMIN".equals(tenant.role());
        UUID usuario = admin ? null : UUID.fromString(tenant.requireUserId());
        var agora = clock.instant();

        var daEmpresa = etapas.listarPorEmpresa(empresa);
        var porEtapa = new LinkedHashMap<UUID, List<NegocioFunil>>();
        for (Etapa etapa : daEmpresa)
            porEtapa.put(etapa.getId(), new ArrayList<>());

        for (NegocioFunil negocio : negocios.listarPorEmpresa(empresa)) {
            var coluna = porEtapa.get(negocio.etapaId());
            if (coluna == null)
                continue;
            var fato = consultaLead.buscar(empresa, negocio.leadId()).orElse(null);
            if (fato == null || !visivel(fato, admin, usuario, filtros))
                continue;
            coluna.add(negocio);
        }

        var colunas = new ArrayList<QuadroFunilDto.Coluna>();
        for (Etapa etapa : daEmpresa) {
            var visiveis = porEtapa.get(etapa.getId());
            var contribuicoes = new ArrayList<ValorComercial>();
            var itens = new ArrayList<QuadroFunilDto.Item>();
            for (NegocioFunil negocio : visiveis) {
                contribuicoes.add(ValorColuna.contribuicao(
                        etapa.tipo(), negocio.valorEstimado(), negocio.valorFechado(), etapa.probabilidade()));
                itens.add(new QuadroFunilDto.Item(
                        negocio.leadId(),
                        etapa.getId(),
                        AtrasoEtapa.atrasado(negocio.entrouNaEtapa(), agora, etapa.sla())));
            }
            colunas.add(new QuadroFunilDto.Coluna(
                    etapa.getId(),
                    etapa.tipo().name(),
                    itens.size(),
                    ValorColuna.somar(contribuicoes).valor(),
                    List.copyOf(itens)));
        }
        return Result.ok(new QuadroFunilDto(List.copyOf(colunas)));
    }

    private static boolean visivel(ConsultaLeadFunil.Fato fato, boolean admin, UUID usuario,
                                   QuadroFunilQuery filtros) {
        if (fato.exclusao())
            return false;
        if (!admin && (fato.dono() == null || !fato.dono().equals(usuario)))
            return false;
        if (filtros.uf() != null && !filtros.uf().equals(fato.uf()))
            return false;
        return filtros.quente() == null || filtros.quente() == fato.quente();
    }
}
