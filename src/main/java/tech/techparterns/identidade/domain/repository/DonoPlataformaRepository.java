package tech.techparterns.identidade.domain.repository;

import java.util.Optional;
import java.util.UUID;

import tech.techparterns.identidade.domain.model.DonoPlataforma;
import tech.techparterns.identidade.domain.model.EmailUsuario;

public interface DonoPlataformaRepository {

    void salvar(DonoPlataforma dono);

    Optional<DonoPlataforma> buscarPorId(UUID id);

    Optional<DonoPlataforma> buscarPorEmail(EmailUsuario email);

    /** O dono único da plataforma (o primeiro cadastrado), se existir. */
    Optional<DonoPlataforma> buscarPrimeiro();

    boolean existeAlgum();
}
