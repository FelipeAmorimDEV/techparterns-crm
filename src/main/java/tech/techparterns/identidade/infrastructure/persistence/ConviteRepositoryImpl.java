package tech.techparterns.identidade.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import tech.techparterns.identidade.domain.model.Convite;
import tech.techparterns.identidade.domain.model.HashToken;
import tech.techparterns.identidade.domain.repository.ConviteRepository;
import tech.techparterns.identidade.infrastructure.persistence.mapper.IdentidadeMapper;

@ApplicationScoped
public class ConviteRepositoryImpl implements ConviteRepository {

    private final EntityManager em;

    public ConviteRepositoryImpl(EntityManager em) { this.em = em; }

    @Override
    @Transactional
    public void salvar(Convite convite) {
        em.merge(IdentidadeMapper.toEntity(convite));
    }

    @Override
    public Optional<Convite> buscarPorHash(HashToken hash) {
        return em.createQuery("from ConviteEntity c where c.tokenHash = :h", ConviteEntity.class)
                .setParameter("h", hash.valor()).getResultStream().findFirst().map(IdentidadeMapper::toDomain);
    }

    @Override
    public Optional<Convite> buscarPorId(UUID id, UUID empresaId) {
        return em.createQuery("from ConviteEntity c where c.id = :id and c.empresaId = :e", ConviteEntity.class)
                .setParameter("id", id).setParameter("e", empresaId)
                .getResultStream().findFirst().map(IdentidadeMapper::toDomain);
    }

    @Override
    public List<Convite> listar(UUID empresaId) {
        return em.createQuery("from ConviteEntity c where c.empresaId = :e order by c.createdAt desc, c.id",
                        ConviteEntity.class)
                .setParameter("e", empresaId).getResultList().stream().map(IdentidadeMapper::toDomain).toList();
    }

    @Override
    @Transactional
    public boolean incrementarUsoSePossivel(UUID conviteId, Instant agora) {
        return em.createQuery("update ConviteEntity c set c.usos = c.usos + 1 where c.id = :id "
                        + "and c.usos < c.maxUsos and c.revogadoEm is null and c.expiraEm > :agora")
                .setParameter("id", conviteId).setParameter("agora", agora).executeUpdate() == 1;
    }
}
