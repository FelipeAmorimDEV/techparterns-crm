package tech.techparterns.identidade.application.dto;

import java.util.List;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Página de resultados.")
public record PaginaDto<T>(List<T> itens, int page, int size, long total) {
}
