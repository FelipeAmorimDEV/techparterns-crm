package tech.techparterns.funil.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import tech.techparterns.funil.domain.model.MotivoPerda;
import tech.techparterns.funil.domain.repository.MotivoPerdaRepository;
import tech.techparterns.funil.infrastructure.persistence.mapper.MotivoPerdaMapper;
import tech.techparterns.shared.domain.CompanyId;

@ApplicationScoped
public class MotivoPerdaRepositoryImpl implements MotivoPerdaRepository {

    private final EntityManager em;

    public MotivoPerdaRepositoryImpl(EntityManager em) {
        this.em = em;
    }

    @Override
    @Transactional
    public Optional<MotivoPerda> buscarPorId(UUID id, CompanyId companyId) {
        return em.createQuery(
                        "from MotivoPerdaEntity e where e.id = :id and e.companyId = :companyId",
                        MotivoPerdaEntity.class)
                .setParameter("id", id)
                .setParameter("companyId", companyId.valor())
                .getResultStream()
                .findFirst()
                .map(MotivoPerdaMapper::toDomain);
    }

    @Override
    @Transactional
    public boolean existeNome(CompanyId companyId, String nome) {
        return !em.createQuery(
                        "select e.id from MotivoPerdaEntity e where e.companyId = :companyId and e.nome = :nome",
                        UUID.class)
                .setParameter("companyId", companyId.valor())
                .setParameter("nome", nome)
                .setMaxResults(1)
                .getResultList()
                .isEmpty();
    }

    @Override
    @Transactional
    public List<MotivoPerda> listarPorEmpresa(CompanyId companyId) {
        return em.createQuery(
                        "from MotivoPerdaEntity e where e.companyId = :companyId", MotivoPerdaEntity.class)
                .setParameter("companyId", companyId.valor())
                .getResultStream()
                .map(MotivoPerdaMapper::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public void salvar(MotivoPerda motivo) {
        em.merge(MotivoPerdaMapper.toEntity(motivo));
    }
}
