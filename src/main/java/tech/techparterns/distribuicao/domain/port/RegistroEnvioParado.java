package tech.techparterns.distribuicao.domain.port;

import java.util.UUID;

public interface RegistroEnvioParado {

    UUID registrarPendente(UUID usuarioId);

    void marcar(UUID envioId, boolean sucesso);
}
