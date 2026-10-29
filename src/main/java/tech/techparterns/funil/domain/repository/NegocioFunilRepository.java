package tech.techparterns.funil.domain.repository;

import java.util.Optional;
import java.util.UUID;

import tech.techparterns.funil.domain.model.NegocioFunil;
import tech.techparterns.shared.domain.CompanyId;

public interface NegocioFunilRepository {

    /** Negócio do lead, só quando a etapa e o lead são da empresa. */
    Optional<NegocioFunil> buscar(UUID leadId, CompanyId companyId);

    /** Há linha de negócio com essa etapa, sem filtrar empresa. */
    boolean existeNaEtapa(UUID etapaId);

    void salvar(NegocioFunil negocio);
}
