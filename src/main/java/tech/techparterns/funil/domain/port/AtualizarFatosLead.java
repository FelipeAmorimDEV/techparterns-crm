package tech.techparterns.funil.domain.port;

import java.util.UUID;

import tech.techparterns.shared.domain.CompanyId;
import tech.techparterns.shared.domain.Result;

/**
 * Grava a situação no lead da empresa, conservando quente, UF e cidade.
 * Não altera o dono. Se a mudança de fatos falhar, nada é salvo.
 */
public interface AtualizarFatosLead {

    Result<Void> atualizar(CompanyId companyId, UUID leadId, String situacao);
}
