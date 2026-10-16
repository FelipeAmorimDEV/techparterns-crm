package tech.techparterns.distribuicao.application;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import tech.techparterns.distribuicao.domain.model.ConfigVendedor;
import tech.techparterns.distribuicao.domain.model.RegraAtribuicao;
import tech.techparterns.distribuicao.domain.port.FatosLead;
import tech.techparterns.distribuicao.domain.port.VendedoresDistribuicao;
import tech.techparterns.distribuicao.domain.repository.DistribuicaoRepository;
import tech.techparterns.distribuicao.domain.service.MotorDistribuicao;

@ApplicationScoped
public class PlanejadorDistribuicao {

    private final DistribuicaoRepository repositorio;
    private final VendedoresDistribuicao vendedores;

    public PlanejadorDistribuicao(DistribuicaoRepository repositorio, VendedoresDistribuicao vendedores) {
        this.repositorio = repositorio;
        this.vendedores = vendedores;
    }

    public record Alvo(UUID id, Instant criadoEm, String uf, String cidade, UUID donoAtual) {}

    public record Decisao(UUID leadId, UUID donoAtual, UUID destino, RegraAtribuicao regra) {}

    public record Plano(List<Decisao> decisoes, Integer ponteiro) {}

    public Plano planejar(String companyId, RegraAtribuicao regra, List<Alvo> alvos) {
        var config = repositorio.buscarConfiguracao(companyId).orElse(null);
        Integer ponteiro = config == null ? null : config.ponteiro();
        var equipe = montar(companyId);
        var territorios = repositorio.listarTerritorios(companyId).stream()
                .map(t -> new MotorDistribuicao.TerritorioRef(t.uf(), t.cidade(), t.vendedorId()))
                .toList();
        var ordenados = alvos.stream()
                .sorted(Comparator.comparing(Alvo::criadoEm).thenComparing(Alvo::id))
                .toList();
        var decisoes = new ArrayList<Decisao>();
        for (var alvo : ordenados) {
            var escolha = MotorDistribuicao.escolher(regra, alvo.uf(), alvo.cidade(), equipe, territorios, ponteiro);
            decisoes.add(new Decisao(alvo.id(), alvo.donoAtual(), escolha.vendedorId(), escolha.regra()));
            ponteiro = escolha.ponteiro();
            equipe = ajustar(equipe, alvo.donoAtual(), escolha.vendedorId());
        }
        return new Plano(decisoes, ponteiro);
    }

    public static List<Alvo> deFatos(List<FatosLead.Fato> fatos, java.util.function.Function<UUID, UUID> dono) {
        return fatos.stream()
                .map(f -> new Alvo(f.id(), f.criadoEm(), f.uf(), f.cidade(), dono.apply(f.id())))
                .toList();
    }

    private List<MotorDistribuicao.Vendedor> montar(String companyId) {
        var configs = repositorio.listarConfigVendedores(companyId);
        var cargas = repositorio.cargasAbertas(companyId);
        return vendedores.listarVendedores(companyId).stream().map(c -> {
            var cfg = configs.stream().filter(x -> x.userId().equals(c.id())).findFirst()
                    .orElse(ConfigVendedor.padrao(companyId, c.id()));
            return new MotorDistribuicao.Vendedor(c.id(), c.criadoEm(), "ACTIVE".equals(c.status()),
                    "SELLER".equals(c.papel()), cfg.participa(), cfg.peso(), cfg.maxLeadsAbertos(),
                    cargas.getOrDefault(c.id(), 0));
        }).toList();
    }

    private static List<MotorDistribuicao.Vendedor> ajustar(List<MotorDistribuicao.Vendedor> equipe, UUID atual, UUID destino) {
        if (Objects.equals(atual, destino))
            return equipe;
        var proxima = new ArrayList<MotorDistribuicao.Vendedor>();
        for (var v : equipe) {
            int delta = 0;
            if (atual != null && v.id().equals(atual))
                delta--;
            if (destino != null && v.id().equals(destino))
                delta++;
            proxima.add(delta == 0 ? v : v.comAbertos(v.abertos() + delta));
        }
        return proxima;
    }
}
