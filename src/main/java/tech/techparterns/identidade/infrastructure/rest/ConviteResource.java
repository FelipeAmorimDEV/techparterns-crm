package tech.techparterns.identidade.infrastructure.rest;

import java.util.List;
import java.util.UUID;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import tech.techparterns.identidade.application.command.AceitarConviteCommand;
import tech.techparterns.identidade.application.command.ConviteIdCommand;
import tech.techparterns.identidade.application.command.CriarConviteCommand;
import tech.techparterns.identidade.application.dto.ConviteListadoDto;
import tech.techparterns.identidade.application.query.ListarConvitesQuery;
import tech.techparterns.identidade.application.usecase.*;

@Path("/convites")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Tag(name = "Convites", description = "Convites para vendedores (SELLER).")
public class ConviteResource {

    private final CriarConviteUseCase criar;
    private final ListarConvitesUseCase listar;
    private final RevogarConviteUseCase revogar;
    private final AceitarConviteUseCase aceitar;

    public ConviteResource(CriarConviteUseCase criar, ListarConvitesUseCase listar, RevogarConviteUseCase revogar,
                           AceitarConviteUseCase aceitar) {
        this.criar = criar;
        this.listar = listar;
        this.revogar = revogar;
        this.aceitar = aceitar;
    }

    @POST
    @RolesAllowed("ADMIN")
    @Operation(summary = "Criar convite", description = "O link completo só aparece nesta resposta.")
    @APIResponses({
            @APIResponse(responseCode = "201", description = "Convite criado."),
            @APIResponse(responseCode = "400", description = "Dados inválidos.")
    })
    public Response criar(CriarConviteCommand cmd) {
        return Response.status(Response.Status.CREATED).entity(Respostas.valor(criar.execute(cmd))).build();
    }

    @GET
    @RolesAllowed("ADMIN")
    @Operation(summary = "Listar convites da empresa", description = "Sem o link.")
    @APIResponses({@APIResponse(responseCode = "200", description = "Convites.")})
    public List<ConviteListadoDto> listar() {
        return Respostas.valor(listar.execute(new ListarConvitesQuery()));
    }

    @POST
    @Path("/{id}/revogar")
    @Consumes(MediaType.WILDCARD)
    @RolesAllowed("ADMIN")
    @Operation(summary = "Revogar convite", description = "Idempotente.")
    @APIResponses({
            @APIResponse(responseCode = "204", description = "Convite revogado."),
            @APIResponse(responseCode = "404", description = "Convite não encontrado.")
    })
    public Response revogar(@PathParam("id") UUID id) {
        Respostas.valor(revogar.execute(new ConviteIdCommand(id)));
        return Response.noContent().build();
    }

    @POST
    @Path("/aceitar")
    @PermitAll
    @Operation(summary = "Aceitar convite", description = "Cria um SELLER ACTIVE. Não abre sessão.")
    @APIResponses({
            @APIResponse(responseCode = "201", description = "Cadastro concluído."),
            @APIResponse(responseCode = "400", description = "Convite inválido ou indisponível, ou dados inválidos.")
    })
    public Response aceitar(AceitarConviteCommand cmd) {
        return Response.status(Response.Status.CREATED).entity(Respostas.valor(aceitar.execute(cmd))).build();
    }
}
