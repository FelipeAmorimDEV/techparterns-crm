package tech.techparterns.identidade.application.service;

import java.time.Clock;
import java.util.UUID;

import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.enterprise.context.ApplicationScoped;

import tech.techparterns.identidade.domain.model.EventoAcesso;
import tech.techparterns.identidade.domain.model.TitularTipo;
import tech.techparterns.identidade.domain.repository.EventoAcessoRepository;

/** Escreve em {@code evento_acesso}. Nunca recebe senha, token, link nem e-mail de terceiros. */
@ApplicationScoped
public class AuditoriaAcesso {

    public static final String SUCESSO = "SUCESSO";
    public static final String FALHA = "FALHA";

    private final EventoAcessoRepository repository;
    private final Clock clock;

    public AuditoriaAcesso(EventoAcessoRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public void registrar(String tipo, String resultado, TitularTipo titularTipo, UUID titularId, UUID empresaId,
                          String ip, String detalhe) {
        repository.salvar(EventoAcesso.registrar(tipo, resultado, titularTipo, titularId, empresaId, ip,
                clock.instant(), detalhe));
    }

    /** Em transação própria: o evento permanece mesmo que a transação do chamador seja revertida. */
    public void registrarIndependente(String tipo, String resultado, TitularTipo titularTipo, UUID titularId,
                                      UUID empresaId, String ip, String detalhe) {
        QuarkusTransaction.requiringNew().run(() -> registrar(tipo, resultado, titularTipo, titularId, empresaId, ip, detalhe));
    }
}
