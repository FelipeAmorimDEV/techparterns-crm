package tech.techparterns.distribuicao.domain.port;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VendedoresDistribuicao {

    record Cadastro(UUID id, String email, String papel, String status, Instant criadoEm) {}

    Optional<Cadastro> buscar(String companyId, UUID userId);

    List<Cadastro> listarVendedores(String companyId);
}
