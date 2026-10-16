package tech.techparterns.identidade.infrastructure.persistence;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import tech.techparterns.identidade.domain.model.EventoAcesso;
import tech.techparterns.identidade.domain.repository.EventoAcessoRepository;
import tech.techparterns.identidade.infrastructure.persistence.mapper.IdentidadeMapper;

@ApplicationScoped
public class EventoAcessoRepositoryImpl implements EventoAcessoRepository {

    private final EntityManager em;

    public EventoAcessoRepositoryImpl(EntityManager em) { this.em = em; }

    @Override
    @Transactional
    public void salvar(EventoAcesso evento) {
        em.merge(IdentidadeMapper.toEntity(evento));
    }
}
