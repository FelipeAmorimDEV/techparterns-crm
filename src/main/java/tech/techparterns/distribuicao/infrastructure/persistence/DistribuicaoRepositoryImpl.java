package tech.techparterns.distribuicao.infrastructure.persistence;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import tech.techparterns.distribuicao.domain.model.Atribuicao;
import tech.techparterns.distribuicao.domain.model.ConfigVendedor;
import tech.techparterns.distribuicao.domain.model.ConfiguracaoEmpresa;
import tech.techparterns.distribuicao.domain.model.DonoLead;
import tech.techparterns.distribuicao.domain.model.RegraAtribuicao;
import tech.techparterns.distribuicao.domain.model.Territorio;
import tech.techparterns.distribuicao.domain.repository.DistribuicaoRepository;

@ApplicationScoped
public class DistribuicaoRepositoryImpl implements DistribuicaoRepository {

    private static final String FILA = """
            SELECT o.lead_id
            FROM lead_ownership o
            JOIN lead l ON l.id = o.lead_id
            WHERE o.company_id = :company
              AND o.owner_user_id IS NULL
              AND l.situacao = 'ABERTA'
              AND l.deleted_at IS NULL
            ORDER BY l.quente DESC,
              CASE
                WHEN l.quente THEN 0
                WHEN (
                  SELECT a.regra FROM assignment a
                  WHERE a.lead_id = l.id AND a.new_owner_id IS NULL
                  ORDER BY a.ocorrido_em DESC, a.id DESC
                  LIMIT 1
                ) IN ('DEVOLUCAO', 'PARADO') THEN 2
                ELSE 1
              END ASC,
              l.created_at ASC,
              l.id ASC
            FOR UPDATE OF o SKIP LOCKED
            LIMIT 1
            """;

    private final EntityManager em;

    public DistribuicaoRepositoryImpl(EntityManager em) {
        this.em = em;
    }

    @Override
    @Transactional
    public Optional<DonoLead> buscarDono(String companyId, UUID leadId) {
        var e = em.find(LeadOwnershipEntity.class, leadId);
        if (e == null || !e.companyId.equals(companyId))
            return Optional.empty();
        return Optional.of(paraDono(e));
    }

    @Override
    @Transactional
    public void salvarDono(DonoLead dono, Instant agora) {
        em.flush();
        var e = em.find(LeadOwnershipEntity.class, dono.leadId());
        if (e == null) {
            e = new LeadOwnershipEntity();
            e.leadId = dono.leadId();
            e.companyId = dono.companyId();
            e.createdAt = agora;
            e.ownerUserId = dono.ownerId();
            e.atribuidoEm = dono.atribuidoEm();
            e.parado = dono.parado();
            e.updatedAt = agora;
            em.persist(e);
            return;
        }
        e.companyId = dono.companyId();
        e.ownerUserId = dono.ownerId();
        e.atribuidoEm = dono.atribuidoEm();
        e.parado = dono.parado();
        e.updatedAt = agora;
    }

    @Override
    @Transactional
    public void marcarParado(String companyId, UUID leadId, Instant agora) {
        var e = em.find(LeadOwnershipEntity.class, leadId);
        if (e == null || !e.companyId.equals(companyId))
            return;
        e.parado = true;
        e.updatedAt = agora;
    }

    @Override
    @Transactional
    public void inserirAtribuicao(Atribuicao a) {
        var e = new AssignmentEntity();
        e.id = a.id();
        e.companyId = a.companyId();
        e.leadId = a.leadId();
        e.previousOwnerId = a.donoAnterior();
        e.newOwnerId = a.donoNovo();
        e.regra = a.regra().name();
        e.motivo = a.motivo();
        e.porUserId = a.autorId();
        e.ocorridoEm = a.ocorridoEm();
        em.persist(e);
    }

    @Override
    @Transactional
    public List<Atribuicao> historico(String companyId, UUID leadId) {
        return em.createQuery("""
                        from AssignmentEntity a
                        where a.companyId = :company and a.leadId = :lead
                        order by a.ocorridoEm asc, a.id asc
                        """, AssignmentEntity.class)
                .setParameter("company", companyId)
                .setParameter("lead", leadId)
                .getResultList().stream()
                .map(DistribuicaoRepositoryImpl::paraAtribuicao)
                .toList();
    }

    @Override
    public Optional<ConfiguracaoEmpresa> buscarConfiguracao(String companyId) {
        var e = em.find(ConfiguracaoEmpresaEntity.class, companyId);
        if (e == null)
            return Optional.empty();
        return Optional.of(new ConfiguracaoEmpresa(e.companyId, e.autoDistribuicao,
                e.regraPadrao == null ? null : RegraAtribuicao.valueOf(e.regraPadrao),
                e.diasParaParado, e.devolverParadoAFila, e.ponteiro));
    }

    @Override
    @Transactional
    public void salvarConfiguracao(ConfiguracaoEmpresa c) {
        var e = em.find(ConfiguracaoEmpresaEntity.class, c.companyId());
        if (e == null) {
            e = new ConfiguracaoEmpresaEntity();
            e.companyId = c.companyId();
        }
        e.autoDistribuicao = c.automatica();
        e.regraPadrao = c.regraPadrao() == null ? null : c.regraPadrao().name();
        e.diasParaParado = c.diasParaParado();
        e.devolverParadoAFila = c.devolverParadoAFila();
        e.ponteiro = c.ponteiro();
        if (!em.contains(e))
            em.persist(e);
    }

    @Override
    @Transactional
    public void salvarPonteiro(String companyId, Integer ponteiro) {
        var atual = buscarConfiguracao(companyId).orElse(ConfiguracaoEmpresa.padrao(companyId));
        salvarConfiguracao(new ConfiguracaoEmpresa(companyId, atual.automatica(), atual.regraPadrao(),
                atual.diasParaParado(), atual.devolverParadoAFila(), ponteiro));
    }

    @Override
    public Optional<ConfigVendedor> buscarConfigVendedor(String companyId, UUID userId) {
        var chave = chave(companyId, userId);
        var e = em.find(ConfigVendedorEntity.class, chave);
        if (e == null)
            return Optional.empty();
        return Optional.of(new ConfigVendedor(companyId, userId, e.peso, e.maxLeadsAbertos, e.participa));
    }

    @Override
    @Transactional
    public void salvarConfigVendedor(ConfigVendedor c) {
        var chave = chave(c.companyId(), c.userId());
        var e = em.find(ConfigVendedorEntity.class, chave);
        if (e == null) {
            e = new ConfigVendedorEntity();
            e.id = chave;
        }
        e.peso = c.peso();
        e.maxLeadsAbertos = c.maxLeadsAbertos();
        e.participa = c.participa();
        if (!em.contains(e))
            em.persist(e);
    }

    @Override
    public List<ConfigVendedor> listarConfigVendedores(String companyId) {
        return em.createQuery("from ConfigVendedorEntity e where e.id.companyId = :company", ConfigVendedorEntity.class)
                .setParameter("company", companyId)
                .getResultList().stream()
                .map(e -> new ConfigVendedor(e.id.companyId, e.id.userId, e.peso, e.maxLeadsAbertos, e.participa))
                .toList();
    }

    @Override
    @Transactional
    public void inserirTerritorio(Territorio t) {
        var e = new TerritorioEntity();
        e.id = t.id();
        e.companyId = t.companyId();
        e.uf = t.uf();
        e.cidade = t.cidade();
        e.userId = t.vendedorId();
        em.persist(e);
        em.flush();
    }

    @Override
    public List<Territorio> listarTerritorios(String companyId) {
        return em.createQuery("from TerritorioEntity t where t.companyId = :company", TerritorioEntity.class)
                .setParameter("company", companyId)
                .getResultList().stream()
                .map(e -> new Territorio(e.id, e.companyId, e.uf, e.cidade, e.userId))
                .toList();
    }

    @Override
    public Map<UUID, Integer> cargasAbertas(String companyId) {
        var linhas = em.createQuery("""
                        select o.ownerUserId, count(o.leadId)
                        from LeadOwnershipEntity o, LeadEntity l
                        where l.id = o.leadId and o.companyId = :company and o.ownerUserId is not null
                          and l.situacao = 'ABERTA' and l.deletedAt is null
                        group by o.ownerUserId
                        """, Object[].class)
                .setParameter("company", companyId)
                .getResultList();
        var mapa = new HashMap<UUID, Integer>();
        for (var linha : linhas)
            mapa.put((UUID) linha[0], ((Number) linha[1]).intValue());
        return mapa;
    }

    @Override
    @Transactional
    public Optional<UUID> travarProximoDaFila(String companyId) {
        var lista = em.createNativeQuery(FILA).setParameter("company", companyId).getResultList();
        if (lista.isEmpty())
            return Optional.empty();
        Object bruto = lista.get(0);
        if (bruto instanceof Object[] arr)
            bruto = arr[0];
        return Optional.of(comoUuid(bruto));
    }

    @Override
    public List<DonoLead> candidatosParado(String companyId, Instant anteriorA) {
        return em.createQuery("""
                        select o from LeadOwnershipEntity o, LeadEntity l
                        where l.id = o.leadId and o.companyId = :company and o.ownerUserId is not null
                          and o.atribuidoEm < :limite and l.situacao = 'ABERTA' and l.deletedAt is null
                        """, LeadOwnershipEntity.class)
                .setParameter("company", companyId)
                .setParameter("limite", anteriorA)
                .getResultList().stream()
                .map(DistribuicaoRepositoryImpl::paraDono)
                .toList();
    }

    @Override
    public Optional<String> ultimaRegraSemDono(UUID leadId) {
        return em.createQuery("""
                        select a.regra from AssignmentEntity a
                        where a.leadId = :lead and a.newOwnerId is null
                        order by a.ocorridoEm desc, a.id desc
                        """, String.class)
                .setParameter("lead", leadId)
                .setMaxResults(1)
                .getResultStream()
                .findFirst();
    }

    private static ConfigVendedorEntity.Chave chave(String companyId, UUID userId) {
        var c = new ConfigVendedorEntity.Chave();
        c.companyId = companyId;
        c.userId = userId;
        return c;
    }

    private static DonoLead paraDono(LeadOwnershipEntity e) {
        return new DonoLead(e.leadId, e.companyId, e.ownerUserId, e.atribuidoEm, e.parado);
    }

    private static Atribuicao paraAtribuicao(AssignmentEntity e) {
        return new Atribuicao(e.id, e.companyId, e.leadId, e.previousOwnerId, e.newOwnerId,
                RegraAtribuicao.valueOf(e.regra), e.motivo, e.porUserId, e.ocorridoEm);
    }

    private static UUID comoUuid(Object bruto) {
        if (bruto instanceof UUID id)
            return id;
        return UUID.fromString(bruto.toString());
    }
}
