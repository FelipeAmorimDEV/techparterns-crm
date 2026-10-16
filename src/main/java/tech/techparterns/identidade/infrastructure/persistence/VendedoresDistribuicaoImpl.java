package tech.techparterns.identidade.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;

import tech.techparterns.distribuicao.domain.port.VendedoresDistribuicao;

@ApplicationScoped
public class VendedoresDistribuicaoImpl implements VendedoresDistribuicao {

    private final EntityManager em;

    public VendedoresDistribuicaoImpl(EntityManager em) {
        this.em = em;
    }

    @Override
    public Optional<Cadastro> buscar(String companyId, UUID userId) {
        var empresa = empresa(companyId);
        if (empresa.isEmpty())
            return Optional.empty();
        return em.createQuery("""
                        from UsuarioEmpresaEntity u
                        where u.id = :id and u.empresaId = :empresa and u.deletedAt is null
                        """, UsuarioEmpresaEntity.class)
                .setParameter("id", userId)
                .setParameter("empresa", empresa.get())
                .getResultStream()
                .findFirst()
                .map(VendedoresDistribuicaoImpl::cadastro);
    }

    @Override
    public List<Cadastro> listarVendedores(String companyId) {
        var empresa = empresa(companyId);
        if (empresa.isEmpty())
            return List.of();
        return em.createQuery("""
                        from UsuarioEmpresaEntity u
                        where u.empresaId = :empresa and u.papel = 'SELLER' and u.deletedAt is null
                        order by u.createdAt asc, u.id asc
                        """, UsuarioEmpresaEntity.class)
                .setParameter("empresa", empresa.get())
                .getResultList().stream()
                .map(VendedoresDistribuicaoImpl::cadastro)
                .toList();
    }

    private static Optional<UUID> empresa(String companyId) {
        try {
            return Optional.of(UUID.fromString(companyId));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private static Cadastro cadastro(UsuarioEmpresaEntity u) {
        return new Cadastro(u.id, u.email, u.papel, u.status, u.createdAt);
    }
}
