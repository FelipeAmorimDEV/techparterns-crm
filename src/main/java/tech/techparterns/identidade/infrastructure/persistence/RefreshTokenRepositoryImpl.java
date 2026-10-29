package tech.techparterns.identidade.infrastructure.persistence;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import tech.techparterns.identidade.domain.model.HashToken;
import tech.techparterns.identidade.domain.model.RefreshToken;
import tech.techparterns.identidade.domain.model.TitularTipo;
import tech.techparterns.identidade.domain.repository.RefreshTokenRepository;
import tech.techparterns.identidade.infrastructure.persistence.mapper.IdentidadeMapper;

@ApplicationScoped
public class RefreshTokenRepositoryImpl implements RefreshTokenRepository {

    private final EntityManager em;

    public RefreshTokenRepositoryImpl(EntityManager em) { this.em = em; }

    @Override
    @Transactional
    public void salvar(RefreshToken token) {
        em.merge(IdentidadeMapper.toEntity(token));
    }

    @Override
    public Optional<RefreshToken> buscarPorHash(HashToken hash) {
        return em.createQuery("from RefreshTokenEntity t where t.tokenHash = :h", RefreshTokenEntity.class)
                .setParameter("h", hash.valor()).getResultStream().findFirst().map(IdentidadeMapper::toDomain);
    }

    @Override
    @Transactional
    public boolean rotacionarSePossivel(UUID tokenId, Instant agora) {
        return em.createQuery("update RefreshTokenEntity t set t.rotacionadoEm = :agora where t.id = :id "
                        + "and t.rotacionadoEm is null and t.revogadoEm is null")
                .setParameter("agora", agora).setParameter("id", tokenId).executeUpdate() == 1;
    }

    @Override
    @Transactional
    public void revogarFamilia(UUID familiaId, Instant agora) {
        em.createQuery("update RefreshTokenEntity t set t.revogadoEm = :agora where t.familiaId = :f "
                        + "and t.revogadoEm is null")
                .setParameter("agora", agora).setParameter("f", familiaId).executeUpdate();
    }

    @Override
    @Transactional
    public void revogarTodasDoTitular(TitularTipo tipo, UUID titularId, Instant agora) {
        em.createQuery("update RefreshTokenEntity t set t.revogadoEm = :agora where t.titularTipo = :tipo "
                        + "and t.titularId = :id and t.revogadoEm is null")
                .setParameter("agora", agora).setParameter("tipo", tipo.name()).setParameter("id", titularId)
                .executeUpdate();
    }
}
