package tech.techparterns.identidade.infrastructure.persistence;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import tech.techparterns.identidade.domain.model.HashToken;
import tech.techparterns.identidade.domain.model.TokenSenha;
import tech.techparterns.identidade.domain.repository.TokenSenhaRepository;
import tech.techparterns.identidade.infrastructure.persistence.mapper.IdentidadeMapper;

@ApplicationScoped
public class TokenSenhaRepositoryImpl implements TokenSenhaRepository {

    private final EntityManager em;

    public TokenSenhaRepositoryImpl(EntityManager em) { this.em = em; }

    @Override
    @Transactional
    public void salvar(TokenSenha token) {
        em.merge(IdentidadeMapper.toEntity(token));
    }

    @Override
    public Optional<TokenSenha> buscarPorHash(HashToken hash) {
        return em.createQuery("from TokenSenhaEntity t where t.tokenHash = :h", TokenSenhaEntity.class)
                .setParameter("h", hash.valor()).getResultStream().findFirst().map(IdentidadeMapper::toDomain);
    }

    @Override
    @Transactional
    public void invalidarPendentesDoUsuario(UUID usuarioId, Instant agora) {
        em.createQuery("update TokenSenhaEntity t set t.invalidadoEm = :agora where t.usuarioId = :u "
                        + "and t.usadoEm is null and t.invalidadoEm is null")
                .setParameter("agora", agora).setParameter("u", usuarioId).executeUpdate();
    }

    @Override
    @Transactional
    public boolean consumirSePossivel(UUID tokenId, Instant agora) {
        return em.createQuery("update TokenSenhaEntity t set t.usadoEm = :agora where t.id = :id "
                        + "and t.usadoEm is null and t.invalidadoEm is null and t.expiraEm > :agora")
                .setParameter("agora", agora).setParameter("id", tokenId).executeUpdate() == 1;
    }
}
