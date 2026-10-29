package tech.techparterns.identidade.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import tech.techparterns.identidade.domain.model.DonoPlataforma;
import tech.techparterns.identidade.domain.model.EmailUsuario;
import tech.techparterns.identidade.domain.repository.DonoPlataformaRepository;
import tech.techparterns.identidade.infrastructure.persistence.mapper.IdentidadeMapper;

@ApplicationScoped
public class DonoPlataformaRepositoryImpl implements DonoPlataformaRepository {

    private final EntityManager em;

    public DonoPlataformaRepositoryImpl(EntityManager em) { this.em = em; }

    @Override
    @Transactional
    public void salvar(DonoPlataforma dono) {
        em.merge(IdentidadeMapper.toEntity(dono));
    }

    @Override
    public Optional<DonoPlataforma> buscarPorId(UUID id) {
        return Optional.ofNullable(em.find(DonoPlataformaEntity.class, id)).map(IdentidadeMapper::toDomain);
    }

    @Override
    public Optional<DonoPlataforma> buscarPorEmail(EmailUsuario email) {
        return em.createQuery("from DonoPlataformaEntity d where d.email = :email", DonoPlataformaEntity.class)
                .setParameter("email", email.valor()).getResultStream().findFirst().map(IdentidadeMapper::toDomain);
    }

    @Override
    public Optional<DonoPlataforma> buscarPrimeiro() {
        return em.createQuery("from DonoPlataformaEntity d order by d.createdAt, d.id", DonoPlataformaEntity.class)
                .setMaxResults(1).getResultStream().findFirst().map(IdentidadeMapper::toDomain);
    }

    @Override
    public boolean existeAlgum() {
        return !em.createQuery("select d.id from DonoPlataformaEntity d", UUID.class)
                .setMaxResults(1).getResultList().isEmpty();
    }
}
