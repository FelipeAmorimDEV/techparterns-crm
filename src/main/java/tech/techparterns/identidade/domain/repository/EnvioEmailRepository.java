package tech.techparterns.identidade.domain.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import tech.techparterns.identidade.domain.model.EnvioEmail;
import tech.techparterns.identidade.domain.model.StatusEnvioEmail;

public interface EnvioEmailRepository {

    void salvar(EnvioEmail envio);

    Optional<EnvioEmail> buscarPorId(UUID id);

    Optional<StatusEnvioEmail> ultimoStatusDoUsuario(UUID usuarioId);

    /** Instante do último envio SETUP registrado para o usuário (intervalo mínimo de 60 s). */
    Optional<Instant> ultimoEnvioSetupEm(UUID usuarioId);

    /** Status do envio mais recente entre os usuários ADMIN PENDING da empresa (DTP-8b). */
    Optional<StatusEnvioEmail> ultimoStatusDosAdminsPending(UUID empresaId);
}
