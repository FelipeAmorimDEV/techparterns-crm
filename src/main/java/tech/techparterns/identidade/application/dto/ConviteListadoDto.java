package tech.techparterns.identidade.application.dto;

import java.time.Instant;
import java.util.UUID;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import tech.techparterns.identidade.domain.model.Convite;

@Schema(description = "Convite listado (sem o link).")
public record ConviteListadoDto(UUID id, String email, Instant expiraEm, int maxUsos, int usos,
                                Instant revogadoEm, Instant criadoEm) {

    public static ConviteListadoDto de(Convite c) {
        return new ConviteListadoDto(c.getId(), c.email() == null ? null : c.email().valor(), c.expiraEm(),
                c.maxUsos(), c.usos(), c.revogadoEm(), c.getCreatedAt());
    }
}
