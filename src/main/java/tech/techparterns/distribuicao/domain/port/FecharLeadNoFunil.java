package tech.techparterns.distribuicao.domain.port;

import java.math.BigDecimal;
import java.util.UUID;

import tech.techparterns.shared.domain.Result;

/**
 * Ganho ou perdido pedido pela escrita de fatos.
 * A empresa e o usuário saem do token na implementação. O cliente não escolhe a empresa.
 */
public interface FecharLeadNoFunil {

    Result<Void> fechar(UUID leadId, String situacao, boolean quente, String uf, String cidade,
                        String planoFechado, BigDecimal valorFechado, UUID motivoPerdaId, String observacao);
}
