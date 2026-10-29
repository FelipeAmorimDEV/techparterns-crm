package tech.techparterns.identidade.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import tech.techparterns.identidade.domain.model.Empresa;

public interface EmpresaRepository {

    void salvar(Empresa empresa);

    Optional<Empresa> buscarPorId(UUID id);

    /** Bloqueia a linha da empresa (FOR UPDATE) até o fim da transação. */
    Optional<Empresa> buscarPorIdBloqueando(UUID id);

    List<ResumoEmpresa> listarResumos();

    /** Empresa com indicadores calculados para a listagem do dono (RF-IA-12). */
    record ResumoEmpresa(Empresa empresa, boolean semAdminAtivo, boolean temAdminPending) {}
}
