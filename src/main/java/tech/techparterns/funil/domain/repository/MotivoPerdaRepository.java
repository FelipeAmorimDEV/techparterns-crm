package tech.techparterns.funil.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import tech.techparterns.funil.domain.model.MotivoPerda;
import tech.techparterns.shared.domain.CompanyId;

public interface MotivoPerdaRepository {

    Optional<MotivoPerda> buscarPorId(UUID id, CompanyId companyId);

    boolean existeNome(CompanyId companyId, String nome);

    List<MotivoPerda> listarPorEmpresa(CompanyId companyId);

    void salvar(MotivoPerda motivo);
}
