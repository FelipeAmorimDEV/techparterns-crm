package tech.techparterns.lead.domain.port;

import tech.techparterns.lead.domain.model.Lead;

/** Chamada só no ramo que cria lead. Completar ou ignorar não passa por aqui. */
public interface DistribuirLeadNovo {

    void aoCriar(Lead lead);
}
