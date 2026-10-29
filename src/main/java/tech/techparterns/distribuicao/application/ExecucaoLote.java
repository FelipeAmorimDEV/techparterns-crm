package tech.techparterns.distribuicao.application;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import tech.techparterns.distribuicao.domain.model.RegraAtribuicao;
import tech.techparterns.distribuicao.domain.repository.DistribuicaoRepository;

@ApplicationScoped
public class ExecucaoLote {

    private final PlanejadorDistribuicao planejador;
    private final TrocarDono trocar;
    private final DistribuicaoRepository repositorio;

    public ExecucaoLote(PlanejadorDistribuicao planejador, TrocarDono trocar, DistribuicaoRepository repositorio) {
        this.planejador = planejador;
        this.trocar = trocar;
        this.repositorio = repositorio;
    }

    public Contratos.ResultadoLote executar(String companyId, RegraAtribuicao regra,
                                            java.util.List<PlanejadorDistribuicao.Alvo> alvos, boolean gravar, UUID autor) {
        var plano = planejador.planejar(companyId, regra, alvos);
        if (gravar) {
            for (var d : plano.decisoes()) {
                if (!Objects.equals(d.donoAtual(), d.destino()))
                    trocar.trocar(companyId, d.leadId(), d.destino(), d.regra(), null, autor);
            }
            var antes = repositorio.buscarConfiguracao(companyId).map(c -> c.ponteiro()).orElse(null);
            if (!Objects.equals(antes, plano.ponteiro()))
                repositorio.salvarPonteiro(companyId, plano.ponteiro());
        }
        var por = new LinkedHashMap<UUID, Integer>();
        int sem = 0;
        for (var d : plano.decisoes()) {
            if (d.destino() == null)
                sem++;
            else
                por.merge(d.destino(), 1, Integer::sum);
        }
        var lista = new ArrayList<Contratos.Contagem>();
        por.forEach((id, qtd) -> lista.add(new Contratos.Contagem(id, qtd)));
        return new Contratos.ResultadoLote(lista, sem);
    }
}
