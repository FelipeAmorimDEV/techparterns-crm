package tech.techparterns.identidade.application.dto;

import java.time.Instant;
import java.util.UUID;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Convite criado. O link completo só aparece nesta resposta.")
public record ConviteCriadoDto(UUID id, String email, Instant expiraEm, int maxUsos, String link) {
}
