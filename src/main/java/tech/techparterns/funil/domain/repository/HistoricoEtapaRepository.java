package tech.techparterns.funil.domain.repository;

import java.util.Optional;
import java.util.UUID;

import tech.techparterns.funil.domain.model.HistoricoEtapa;
import tech.techparterns.shared.domain.CompanyId;

public interface HistoricoEtapaRepository {

    /** Lê pelo id quando a etapa de destino é da empresa. */
    Optional<HistoricoEtapa> buscarPorId(UUID id, CompanyId companyId);

    void salvar(HistoricoEtapa historico);
}
