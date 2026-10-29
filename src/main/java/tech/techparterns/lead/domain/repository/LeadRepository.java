package tech.techparterns.lead.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import tech.techparterns.lead.domain.model.Lead;
import tech.techparterns.shared.domain.CompanyId;

public interface LeadRepository {

    /** Filtra id e empresa e inclui lead já excluído. */
    Optional<Lead> buscarPorId(UUID id, CompanyId companyId);

    /** Somente leads da empresa com {@code deletedAt} nulo. */
    List<Lead> listarNaoExcluidos(CompanyId companyId);

    void salvar(Lead lead);
}
