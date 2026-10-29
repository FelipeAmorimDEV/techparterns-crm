package tech.techparterns.funil.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import tech.techparterns.funil.domain.model.Etapa;
import tech.techparterns.funil.domain.repository.EtapaRepository;
import tech.techparterns.funil.infrastructure.persistence.mapper.EtapaMapper;
import tech.techparterns.shared.domain.CompanyId;

@ApplicationScoped
public class EtapaRepositoryImpl implements EtapaRepository {

    private final EntityManager em;

    public EtapaRepositoryImpl(EntityManager em) {
        this.em = em;
    }

    @Override
    @Transactional
    public Optional<Etapa> buscarPorId(UUID id, CompanyId companyId) {
        return em.createQuery(
                        "from EtapaEntity e where e.id = :id and e.companyId = :companyId", EtapaEntity.class)
                .setParameter("id", id)
                .setParameter("companyId", companyId.valor())
                .getResultStream()
                .findFirst()
                .map(EtapaMapper::toDomain);
    }

    @Override
    @Transactional
    public List<Etapa> listarPorEmpresa(CompanyId companyId) {
        return em.createQuery(
                        "from EtapaEntity e where e.companyId = :companyId", EtapaEntity.class)
                .setParameter("companyId", companyId.valor())
                .getResultStream()
                .map(EtapaMapper::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public void salvar(Etapa etapa) {
        em.merge(EtapaMapper.toEntity(etapa));
    }
}
