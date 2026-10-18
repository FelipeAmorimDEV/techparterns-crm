package tech.techparterns.identidade.application.dto;

import java.time.Instant;
import java.util.UUID;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonInclude;

import tech.techparterns.identidade.domain.model.StatusEnvioEmail;
import tech.techparterns.identidade.domain.model.UsuarioEmpresa;

@Schema(description = "Usuário da empresa. Nunca traz hash de senha, tokens nem contadores.")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UsuarioListadoDto(UUID id, String nome, String email, String telefone, String role, String status,
                                Instant ultimoLoginEm, Instant criadoEm,
                                @Schema(description = "Só para usuário PENDING: PENDENTE, ENVIADO ou FALHA.")
                                String statusEnvioEmail) {

    public static UsuarioListadoDto de(UsuarioEmpresa u, StatusEnvioEmail envio) {
        return new UsuarioListadoDto(u.getId(), u.nome().valor(), u.email().valor(),
                u.telefone() == null ? null : u.telefone().valor(), u.papel().name(), u.status().name(),
                u.ultimoLoginEm(), u.getCreatedAt(), envio == null ? null : envio.name());
    }
}
