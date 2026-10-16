package tech.techparterns.identidade.infrastructure.persistence;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;

import tech.techparterns.identidade.domain.model.EmailUsuario;
import tech.techparterns.identidade.domain.repository.EmailRegistradoRepository;

@ApplicationScoped
public class EmailRegistradoRepositoryImpl implements EmailRegistradoRepository {

    private final EntityManager em;

    public EmailRegistradoRepositoryImpl(EntityManager em) { this.em = em; }

    @Override
    public boolean emUso(EmailUsuario email) {
        return existe("select u.id from UsuarioEmpresaEntity u where u.email = :e", email)
                || existe("select d.id from DonoPlataformaEntity d where d.email = :e", email);
    }

    private boolean existe(String jpql, EmailUsuario email) {
        return !em.createQuery(jpql, UUID.class).setParameter("e", email.valor()).setMaxResults(1)
                .getResultList().isEmpty();
    }
}
