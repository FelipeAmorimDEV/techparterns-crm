package tech.techparterns.funil.infrastructure.rest;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import tech.techparterns.funil.application.dto.QuadroFunilDto;
import tech.techparterns.funil.application.query.QuadroFunilQuery;
import tech.techparterns.funil.application.usecase.QuadroFunilUseCase;

@Path("/funil/quadro")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Tag(name = "Quadro do funil", description = "Quadro da empresa autenticada.")
public class QuadroFunilResource {

    private final QuadroFunilUseCase quadro;

    public QuadroFunilResource(QuadroFunilUseCase quadro) {
        this.quadro = quadro;
    }

    @GET
    @RolesAllowed({"ADMIN", "SELLER"})
    @Operation(summary = "Ver o quadro", description = "Colunas e leads visíveis da empresa do token.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Quadro da empresa.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = QuadroFunilDto.class)))
    })
    public QuadroFunilDto quadro(
            @Parameter(description = "UF já gravada no lead. Omitida, não restringe.")
            @QueryParam("uf") String uf,
            @Parameter(description = "Marcador quente já gravado. Omitido, não restringe.")
            @QueryParam("quente") Boolean quente) {
        return quadro.execute(new QuadroFunilQuery(uf, quente)).getValue();
    }
}
