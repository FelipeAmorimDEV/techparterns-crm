package tech.techparterns.identidade.infrastructure.persistence;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import tech.techparterns.identidade.domain.repository.ControleLoginRepository;
import tech.techparterns.identidade.domain.service.PoliticaBloqueioLogin;

@ApplicationScoped
public class ControleLoginRepositoryImpl implements ControleLoginRepository {

    private final EntityManager em;

    public ControleLoginRepositoryImpl(EntityManager em) { this.em = em; }

    private static String chavePar(String emailHash, String ip) { return emailHash + "|" + ip; }

    @Override
    public boolean estaBloqueado(String emailHash, String ip, Instant agora) {
        Number n = (Number) em.createNativeQuery("select count(*) from login_bloqueio where bloqueado_ate > :agora and "
                        + "((tipo = 'PAR' and chave = :par) or (tipo = 'EMAIL' and chave = :email) or (tipo = 'IP' and chave = :ip))")
                .setParameter("agora", agora).setParameter("par", chavePar(emailHash, ip))
                .setParameter("email", emailHash).setParameter("ip", ip).getSingleResult();
        return n.longValue() > 0;
    }

    @Override
    @Transactional
    public boolean registrarFalha(String emailHash, String ip, Instant agora, PoliticaBloqueioLogin politica) {
        em.createNativeQuery("insert into login_falha (id, email_hash, ip, ocorrido_em) values (:id, :h, :ip, :t)")
                .setParameter("id", UUID.randomUUID()).setParameter("h", emailHash).setParameter("ip", ip)
                .setParameter("t", agora).executeUpdate();
        boolean disparou = false;
        Instant ate = politica.bloqueadoAte(agora);

        // Par: 5 falhas sem janela
        String par = chavePar(emailHash, ip);
        Number falhas = (Number) em.createNativeQuery("insert into login_bloqueio (tipo, chave, falhas) values ('PAR', :k, 1) "
                        + "on conflict (tipo, chave) do update set falhas = login_bloqueio.falhas + 1 returning falhas")
                .setParameter("k", par).getSingleResult();
        if (politica.parDeveBloquear(falhas.intValue())) {
            em.createNativeQuery("update login_bloqueio set falhas = 0, bloqueado_ate = :ate where tipo = 'PAR' and chave = :k")
                    .setParameter("ate", ate).setParameter("k", par).executeUpdate();
            disparou = true;
        }
        disparou |= verificarJanela("EMAIL", emailHash, "email_hash", emailHash, agora, ate, politica);
        disparou |= verificarJanela("IP", ip, "ip", ip, agora, ate, politica);
        return disparou;
    }

    private boolean verificarJanela(String tipo, String chave, String coluna, String valor, Instant agora,
                                    Instant ate, PoliticaBloqueioLogin politica) {
        var zerado = em.createNativeQuery("select zerado_em from login_bloqueio where tipo = :t and chave = :k")
                .setParameter("t", tipo).setParameter("k", chave).getResultList();
        Instant zeradoEm = zerado.isEmpty() || zerado.get(0) == null ? null : paraInstant(zerado.get(0));
        Instant inicio = politica.inicioDaJanela(agora, zeradoEm);
        long total = ((Number) em.createNativeQuery("select count(*) from login_falha where " + coluna
                        + " = :v and ocorrido_em >= :inicio")
                .setParameter("v", valor).setParameter("inicio", inicio).getSingleResult()).longValue();
        boolean deve = tipo.equals("EMAIL") ? politica.emailDeveBloquear(total) : politica.ipDeveBloquear(total);
        if (!deve)
            return false;
        em.createNativeQuery("insert into login_bloqueio (tipo, chave, falhas, bloqueado_ate, zerado_em) "
                        + "values (:t, :k, 0, :ate, :ate) on conflict (tipo, chave) do update "
                        + "set bloqueado_ate = :ate, zerado_em = :ate")
                .setParameter("t", tipo).setParameter("k", chave).setParameter("ate", ate).executeUpdate();
        return true;
    }

    private static Instant paraInstant(Object o) {
        if (o instanceof Instant i) return i;
        if (o instanceof java.time.OffsetDateTime odt) return odt.toInstant();
        if (o instanceof Timestamp ts) return ts.toInstant();
        throw new IllegalStateException("Tipo de data inesperado: " + o.getClass());
    }

    @Override
    @Transactional
    public void zerarPar(String emailHash, String ip) {
        em.createNativeQuery("update login_bloqueio set falhas = 0 where tipo = 'PAR' and chave = :k")
                .setParameter("k", chavePar(emailHash, ip)).executeUpdate();
    }
}
