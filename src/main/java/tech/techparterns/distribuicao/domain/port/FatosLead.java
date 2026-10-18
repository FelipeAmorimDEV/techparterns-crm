package tech.techparterns.distribuicao.domain.port;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FatosLead {

    record Fato(UUID id, String companyId, String nome, String situacao, String uf, String cidade, boolean quente,
                String origem, Instant criadoEm, boolean excluido) {}

    record Filtro(String uf, String cidade, Boolean quente, String origem) {}

    Optional<Fato> buscar(String companyId, UUID leadId);

    List<Fato> listarSemDono(String companyId, Filtro filtro);

    List<Fato> listarCarteiraAberta(String companyId, UUID donoId);

    void atualizar(String companyId, UUID leadId, String situacao, boolean quente, String uf, String cidade);
}
