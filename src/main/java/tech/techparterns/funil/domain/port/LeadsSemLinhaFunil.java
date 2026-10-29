package tech.techparterns.funil.domain.port;

import java.util.List;
import java.util.UUID;

/** Leads não excluídos, de todas as empresas, que ainda não têm linha em funil_negocio. */
public interface LeadsSemLinhaFunil {

    record LeadSemLinha(UUID leadId, String companyId, String situacao) {}

    List<LeadSemLinha> listar();
}
