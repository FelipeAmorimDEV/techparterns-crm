package tech.techparterns.identidade.application.query;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Listagem paginada de usuários da própria empresa.")
public record ListarUsuariosQuery(
        @Schema(description = "Página, a partir de 0.") Integer page,
        @Schema(description = "Tamanho da página (1 a 100, padrão 20).") Integer size,
        @Schema(description = "Filtro por status.") String status,
        @Schema(description = "Filtro por papel.") String role) {
}
