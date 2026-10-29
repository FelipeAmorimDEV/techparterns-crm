package tech.techparterns.funil.domain.port;

import java.util.Optional;
import java.util.UUID;

import tech.techparterns.shared.domain.CompanyId;

public interface ConsultaLeadFunil {

    record Fato(String situacao, String uf, String cidade, boolean quente, boolean exclusao, UUID dono) {}

    /** Vazio quando o lead não existe ou é de outra empresa. */
    Optional<Fato> buscar(CompanyId companyId, UUID leadId);
}
