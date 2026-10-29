package tech.techparterns.distribuicao.application;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import tech.techparterns.distribuicao.domain.model.ConfiguracaoEmpresa;
import tech.techparterns.lead.domain.model.Lead;
import tech.techparterns.lead.domain.port.DistribuirLeadNovo;

@ApplicationScoped
public class DistribuirLeadNovoImpl implements DistribuirLeadNovo {

    private final TrocarDono trocar;
    private final ExecucaoLote lote;
    private final tech.techparterns.distribuicao.domain.repository.DistribuicaoRepository repositorio;
    private final EntityManager em;

    public DistribuirLeadNovoImpl(TrocarDono trocar, ExecucaoLote lote,
                                  tech.techparterns.distribuicao.domain.repository.DistribuicaoRepository repositorio,
                                  EntityManager em) {
        this.trocar = trocar;
        this.lote = lote;
        this.repositorio = repositorio;
        this.em = em;
    }

    @Override
    @Transactional
    public void aoCriar(Lead lead) {
        em.flush();
        var empresa = lead.companyId().valor();
        trocar.garantirSemDono(empresa, lead.getId());
        var config = repositorio.buscarConfiguracao(empresa).orElse(ConfiguracaoEmpresa.padrao(empresa));
        if (!config.automatica() || config.regraPadrao() == null || !config.regraPadrao().regraDeLote())
            return;
        var alvo = new PlanejadorDistribuicao.Alvo(lead.getId(), lead.getCreatedAt(), lead.uf(), lead.cidade(), null);
        lote.executar(empresa, config.regraPadrao(), List.of(alvo), true, null);
    }
}
