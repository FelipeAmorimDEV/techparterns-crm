package tech.techparterns.funil.infrastructure.rest;

import java.util.UUID;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import tech.techparterns.funil.application.command.AlterarEtapaCommand;
import tech.techparterns.funil.application.command.ApagarEtapaCommand;
import tech.techparterns.funil.application.command.CriarEtapaCommand;
import tech.techparterns.funil.application.command.DesativarEtapaCommand;
import tech.techparterns.funil.application.dto.EtapaCriadaDto;
import tech.techparterns.funil.application.usecase.AlterarEtapaUseCase;
import tech.techparterns.funil.application.usecase.ApagarEtapaUseCase;
import tech.techparterns.funil.application.usecase.CriarEtapaUseCase;
import tech.techparterns.funil.application.usecase.DesativarEtapaUseCase;
import tech.techparterns.shared.domain.exception.DomainException;

@Path("/funil/etapas")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Tag(name = "Etapas do funil", description = "Administração das etapas da empresa autenticada.")
public class EtapaFunilResource {

    private final CriarEtapaUseCase criar;
    private final AlterarEtapaUseCase alterar;
    private final DesativarEtapaUseCase desativar;
    private final ApagarEtapaUseCase apagar;

    public EtapaFunilResource(CriarEtapaUseCase criar, AlterarEtapaUseCase alterar, DesativarEtapaUseCase desativar,
                              ApagarEtapaUseCase apagar) {
        this.criar = criar;
        this.alterar = alterar;
        this.desativar = desativar;
        this.apagar = apagar;
    }

    @POST
    @RolesAllowed("ADMIN")
    @Operation(summary = "Criar etapa", description = "Cria uma etapa na empresa do token.")
    @APIResponses({
            @APIResponse(responseCode = "201", description = "Etapa criada.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = EtapaCriadaDto.class))),
            @APIResponse(responseCode = "400", description = "Requisição inválida.")
    })
    public Response criar(
            @RequestBody(required = true,
                    content = @Content(schema = @Schema(implementation = CriarEtapaCommand.class)))
            CriarEtapaCommand cmd) {
        var resultado = criar.execute(cmd);
        if (resultado.isFailure())
            throw new DomainException(resultado.getErrors());
        return Response.status(Response.Status.CREATED)
                .entity(new EtapaCriadaDto(resultado.getValue()))
                .build();
    }

    @PATCH
    @Path("/{id}")
    @RolesAllowed("ADMIN")
    @Operation(summary = "Alterar etapa",
            description = "Altera nome, ordem, probabilidade, cor e prazo da etapa da empresa do token.")
    @APIResponses({
            @APIResponse(responseCode = "204", description = "Etapa alterada."),
            @APIResponse(responseCode = "400", description = "Requisição inválida.")
    })
    public Response alterar(
            @PathParam("id") UUID id,
            @RequestBody(required = true,
                    content = @Content(schema = @Schema(implementation = AlterarEtapaCommand.class)))
            AlterarEtapaCommand cmd) {
        var corpo = cmd == null
                ? new AlterarEtapaCommand(id, null, null, null, null, null)
                : new AlterarEtapaCommand(id, cmd.nome(), cmd.ordem(), cmd.probabilidade(), cmd.cor(), cmd.slaDias());
        var resultado = alterar.execute(corpo);
        if (resultado.isFailure())
            throw new DomainException(resultado.getErrors());
        return Response.noContent().build();
    }

    @POST
    @Path("/{id}/desativar")
    @RolesAllowed("ADMIN")
    @Operation(summary = "Desativar etapa", description = "Desativa a etapa da empresa do token.")
    @APIResponses({
            @APIResponse(responseCode = "204", description = "Etapa desativada."),
            @APIResponse(responseCode = "400", description = "Requisição inválida.")
    })
    public Response desativar(@PathParam("id") UUID id) {
        var resultado = desativar.execute(new DesativarEtapaCommand(id));
        if (resultado.isFailure())
            throw new DomainException(resultado.getErrors());
        return Response.noContent().build();
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed("ADMIN")
    @Operation(summary = "Apagar etapa", description = "Exclusão lógica da etapa da empresa do token.")
    @APIResponses({
            @APIResponse(responseCode = "204", description = "Etapa apagada."),
            @APIResponse(responseCode = "400", description = "Requisição inválida.")
    })
    public Response apagar(@PathParam("id") UUID id) {
        var resultado = apagar.execute(new ApagarEtapaCommand(id));
        if (resultado.isFailure())
            throw new DomainException(resultado.getErrors());
        return Response.noContent().build();
    }
}
