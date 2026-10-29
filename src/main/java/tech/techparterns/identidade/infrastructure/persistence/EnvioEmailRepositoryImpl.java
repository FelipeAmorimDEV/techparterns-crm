package tech.techparterns.identidade.infrastructure.persistence;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import tech.techparterns.identidade.domain.model.EnvioEmail;
import tech.techparterns.identidade.domain.model.StatusEnvioEmail;
import tech.techparterns.identidade.domain.repository.EnvioEmailRepository;
import tech.techparterns.identidade.infrastructure.persistence.mapper.IdentidadeMapper;

@ApplicationScoped
public class EnvioEmailRepositoryImpl implements EnvioEmailRepository {

    private final EntityManager em;

    public EnvioEmailRepositoryImpl(EntityManager em) { this.em = em; }

    @Override
    @Transactional
    public void salvar(EnvioEmail envio) {
        em.merge(IdentidadeMapper.toEntity(envio));
    }

    @Override
    public Optional<EnvioEmail> buscarPorId(UUID id) {
        return Optional.ofNullable(em.find(EnvioEmailEntity.class, id)).map(IdentidadeMapper::toDomain);
    }

    @Override
    public Optional<StatusEnvioEmail> ultimoStatusDoUsuario(UUID usuarioId) {
        return em.createQuery("select e.status from EnvioEmailEntity e where e.usuarioId = :u "
                        + "order by e.createdAt desc, e.id desc", String.class)
                .setParameter("u", usuarioId).setMaxResults(1).getResultStream().findFirst()
                .map(StatusEnvioEmail::valueOf);
    }

    @Override
    public Optional<Instant> ultimoEnvioSetupEm(UUID usuarioId) {
        return em.createQuery("select e.createdAt from EnvioEmailEntity e where e.usuarioId = :u and e.tipo = 'SETUP' "
                        + "order by e.createdAt desc", Instant.class)
                .setParameter("u", usuarioId).setMaxResults(1).getResultStream().findFirst();
    }

    @Override
    public Optional<StatusEnvioEmail> ultimoStatusDosAdminsPending(UUID empresaId) {
        return em.createQuery("select e.status from EnvioEmailEntity e, UsuarioEmpresaEntity u "
                        + "where e.usuarioId = u.id and u.empresaId = :emp and u.papel = 'ADMIN' and u.status = 'PENDING' "
                        + "order by e.createdAt desc, e.id desc", String.class)
                .setParameter("emp", empresaId).setMaxResults(1).getResultStream().findFirst()
                .map(StatusEnvioEmail::valueOf);
    }
}
