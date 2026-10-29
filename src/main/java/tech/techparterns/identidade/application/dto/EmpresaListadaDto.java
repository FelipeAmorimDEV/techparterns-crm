package tech.techparterns.identidade.application.dto;

import java.time.Instant;
import java.util.UUID;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonInclude;

@Schema(description = "Empresa na visão do dono da plataforma.")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record EmpresaListadaDto(UUID id, String nome, String status, Instant suspensaEm, String motivoSuspensao,
                                boolean semAdminAtivo, boolean temAdminPending,
                                @Schema(description = "Status do último envio ao admin PENDING (omitido se não há).")
                                String statusEnvioEmailAdminPending) {
}
