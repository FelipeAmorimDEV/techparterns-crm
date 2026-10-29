package tech.techparterns.identidade.application.dto;

import java.util.UUID;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import tech.techparterns.identidade.domain.model.UsuarioEmpresa;

@Schema(description = "Perfil do usuário autenticado.")
public record PerfilDto(UUID id, String nome, String email, String telefone, String role, String status,
                        UUID companyId) {

    public static PerfilDto de(UsuarioEmpresa u) {
        return new PerfilDto(u.getId(), u.nome().valor(), u.email().valor(),
                u.telefone() == null ? null : u.telefone().valor(), u.papel().name(), u.status().name(),
                u.empresaId());
    }
}
