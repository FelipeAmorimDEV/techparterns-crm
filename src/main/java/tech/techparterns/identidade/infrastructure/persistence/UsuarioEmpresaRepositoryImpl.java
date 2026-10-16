package tech.techparterns.identidade.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;

import tech.techparterns.identidade.domain.model.EmailUsuario;
import tech.techparterns.identidade.domain.model.PapelUsuario;
import tech.techparterns.identidade.domain.model.StatusUsuario;
import tech.techparterns.identidade.domain.model.UsuarioEmpresa;
import tech.techparterns.identidade.domain.repository.UsuarioEmpresaRepository;
import tech.techparterns.identidade.infrastructure.persistence.mapper.IdentidadeMapper;

@ApplicationScoped
public class UsuarioEmpresaRepositoryImpl implements UsuarioEmpresaRepository {

    private final EntityManager em;

    public UsuarioEmpresaRepositoryImpl(EntityManager em) { this.em = em; }

    @Override
    @Transactional
    public void salvar(UsuarioEmpresa usuario) {
        em.merge(IdentidadeMapper.toEntity(usuario));
    }

    @Override
    public Optional<UsuarioEmpresa> buscarPorId(UUID id, UUID empresaId) {
        return em.createQuery("from UsuarioEmpresaEntity u where u.id = :id and u.empresaId = :empresaId",
                        UsuarioEmpresaEntity.class)
                .setParameter("id", id).setParameter("empresaId", empresaId)
                .getResultStream().findFirst().map(IdentidadeMapper::toDomain);
    }

    @Override
    public Optional<UsuarioEmpresa> buscarPorIdSemEmpresa(UUID id) {
        return Optional.ofNullable(em.find(UsuarioEmpresaEntity.class, id)).map(IdentidadeMapper::toDomain);
    }

    @Override
    public Optional<UsuarioEmpresa> buscarPorEmail(EmailUsuario email) {
        return em.createQuery("from UsuarioEmpresaEntity u where u.email = :email", UsuarioEmpresaEntity.class)
                .setParameter("email", email.valor())
                .getResultStream().findFirst().map(IdentidadeMapper::toDomain);
    }

    @Override
    public boolean existeEmail(EmailUsuario email) {
        return !em.createQuery("select u.id from UsuarioEmpresaEntity u where u.email = :email", UUID.class)
                .setParameter("email", email.valor()).setMaxResults(1).getResultList().isEmpty();
    }

    @Override
    @Transactional
    public List<UsuarioEmpresa> bloquearAdminsDaEmpresa(UUID empresaId) {
        // Ordenado por id: duas transações disputando os mesmos admins travam na mesma ordem.
        return em.createQuery("from UsuarioEmpresaEntity u where u.empresaId = :empresaId and u.papel = 'ADMIN' order by u.id",
                        UsuarioEmpresaEntity.class)
                .setParameter("empresaId", empresaId)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultList().stream().map(IdentidadeMapper::toDomain).toList();
    }

    @Override
    public List<UsuarioEmpresa> listar(UUID empresaId, StatusUsuario status, PapelUsuario papel, int page, int size) {
        var q = em.createQuery("from UsuarioEmpresaEntity u where " + filtro(status, papel) + " order by u.nome, u.id",
                UsuarioEmpresaEntity.class);
        parametros(q, empresaId, status, papel);
        return q.setFirstResult(page * size).setMaxResults(size).getResultList()
                .stream().map(IdentidadeMapper::toDomain).toList();
    }

    @Override
    public long contar(UUID empresaId, StatusUsuario status, PapelUsuario papel) {
        var q = em.createQuery("select count(u) from UsuarioEmpresaEntity u where " + filtro(status, papel), Long.class);
        parametros(q, empresaId, status, papel);
        return q.getSingleResult();
    }

    private static String filtro(StatusUsuario status, PapelUsuario papel) {
        return "u.empresaId = :empresaId" + (status != null ? " and u.status = :status" : "")
                + (papel != null ? " and u.papel = :papel" : "");
    }

    private static void parametros(jakarta.persistence.Query q, UUID empresaId, StatusUsuario status, PapelUsuario papel) {
        q.setParameter("empresaId", empresaId);
        if (status != null) q.setParameter("status", status.name());
        if (papel != null) q.setParameter("papel", papel.name());
    }
}
