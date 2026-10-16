package tech.techparterns.distribuicao.domain.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import tech.techparterns.distribuicao.domain.model.Atribuicao;
import tech.techparterns.distribuicao.domain.model.ConfigVendedor;
import tech.techparterns.distribuicao.domain.model.ConfiguracaoEmpresa;
import tech.techparterns.distribuicao.domain.model.DonoLead;
import tech.techparterns.distribuicao.domain.model.Territorio;

public interface DistribuicaoRepository {

    Optional<DonoLead> buscarDono(String companyId, UUID leadId);

    void salvarDono(DonoLead dono, Instant agora);

    void marcarParado(String companyId, UUID leadId, Instant agora);

    void inserirAtribuicao(Atribuicao atribuicao);

    List<Atribuicao> historico(String companyId, UUID leadId);

    Optional<ConfiguracaoEmpresa> buscarConfiguracao(String companyId);

    void salvarConfiguracao(ConfiguracaoEmpresa configuracao);

    void salvarPonteiro(String companyId, Integer ponteiro);

    Optional<ConfigVendedor> buscarConfigVendedor(String companyId, UUID userId);

    void salvarConfigVendedor(ConfigVendedor config);

    List<ConfigVendedor> listarConfigVendedores(String companyId);

    void inserirTerritorio(Territorio territorio);

    List<Territorio> listarTerritorios(String companyId);

    java.util.Map<UUID, Integer> cargasAbertas(String companyId);

    Optional<UUID> travarProximoDaFila(String companyId);

    List<DonoLead> candidatosParado(String companyId, Instant anteriorA);

    /** Regra da última ida à fila (dono novo vazio), ou vazio. */
    Optional<String> ultimaRegraSemDono(UUID leadId);
}
