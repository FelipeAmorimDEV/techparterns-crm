package tech.techparterns.identidade.application.dto;

import java.util.UUID;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Empresa criada com o primeiro admin PENDING.")
public record EmpresaCriadaDto(UUID id, String nome, String status, UUID adminId) {
}
