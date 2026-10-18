package tech.techparterns.distribuicao.infrastructure.mail;

import java.time.Instant;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import io.quarkus.narayana.jta.QuarkusTransaction;

import tech.techparterns.distribuicao.domain.port.RegistroEnvioParado;
import tech.techparterns.identidade.infrastructure.persistence.EnvioEmailEntity;

@ApplicationScoped
public class RegistroEnvioParadoImpl implements RegistroEnvioParado {

    private final EntityManager em;

    public RegistroEnvioParadoImpl(EntityManager em) {
        this.em = em;
    }

    @Override
    @Transactional
    public UUID registrarPendente(UUID usuarioId) {
        var agora = Instant.now();
        var e = new EnvioEmailEntity();
        e.id = UUID.randomUUID();
        e.usuarioId = usuarioId;
        e.tipo = "PARADO";
        e.status = "PENDENTE";
        e.tentativas = 0;
        e.createdAt = agora;
        e.updatedAt = agora;
        em.persist(e);
        return e.id;
    }

    @Override
    public void marcar(UUID envioId, boolean sucesso) {
        QuarkusTransaction.requiringNew().run(() -> {
            var e = em.find(EnvioEmailEntity.class, envioId);
            if (e == null)
                return;
            e.tentativas = e.tentativas + 1;
            e.status = sucesso ? "ENVIADO" : "FALHA";
            e.updatedAt = Instant.now();
        });
    }
}
