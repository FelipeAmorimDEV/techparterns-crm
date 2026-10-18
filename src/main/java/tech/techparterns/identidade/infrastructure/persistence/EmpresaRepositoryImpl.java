package tech.techparterns.identidade.infrastructure.persistence;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;

import tech.techparterns.identidade.domain.model.Empresa;
import tech.techparterns.identidade.domain.repository.EmpresaRepository;
import tech.techparterns.identidade.infrastructure.persistence.mapper.IdentidadeMapper;

@ApplicationScoped
public class EmpresaRepositoryImpl implements EmpresaRepository {

    private final EntityManager em;

    public EmpresaRepositoryImpl(EntityManager em) { this.em = em; }

    @Override
    @Transactional
    public void salvar(Empresa empresa) {
        em.merge(IdentidadeMapper.toEntity(empresa));
    }

    @Override
    public Optional<Empresa> buscarPorId(UUID id) {
        return Optional.ofNullable(em.find(EmpresaEntity.class, id)).map(IdentidadeMapper::toDomain);
    }

    @Override
    @Transactional
    public Optional<Empresa> buscarPorIdBloqueando(UUID id) {
        return Optional.ofNullable(em.find(EmpresaEntity.class, id, LockModeType.PESSIMISTIC_WRITE))
                .map(IdentidadeMapper::toDomain);
    }

    @Override
    public List<ResumoEmpresa> listarResumos() {
        var empresas = em.createQuery("from EmpresaEntity e order by e.nome, e.id", EmpresaEntity.class).getResultList();
        Set<UUID> comAdminAtivo = new HashSet<>(em.createQuery(
                "select distinct u.empresaId from UsuarioEmpresaEntity u where u.papel = 'ADMIN' and u.status = 'ACTIVE'",
                UUID.class).getResultList());
        Set<UUID> comAdminPending = new HashSet<>(em.createQuery(
                "select distinct u.empresaId from UsuarioEmpresaEntity u where u.papel = 'ADMIN' and u.status = 'PENDING'",
                UUID.class).getResultList());
        return empresas.stream()
                .map(e -> new ResumoEmpresa(IdentidadeMapper.toDomain(e), !comAdminAtivo.contains(e.id),
                        comAdminPending.contains(e.id)))
                .toList();
    }
}
