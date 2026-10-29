package tech.techparterns.funil.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import tech.techparterns.funil.domain.model.Etapa;
import tech.techparterns.shared.domain.CompanyId;

public interface EtapaRepository {

    Optional<Etapa> buscarPorId(UUID id, CompanyId companyId);

    List<Etapa> listarPorEmpresa(CompanyId companyId);

    void salvar(Etapa etapa);
}
