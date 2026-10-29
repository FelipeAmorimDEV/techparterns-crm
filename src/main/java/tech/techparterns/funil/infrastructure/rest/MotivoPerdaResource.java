package tech.techparterns.funil.infrastructure.rest;

import java.util.List;
import java.util.UUID;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import tech.techparterns.funil.application.command.AlterarAtivoMotivoPerdaCommand;
import tech.techparterns.funil.application.command.CadastrarMotivoPerdaCommand;
import tech.techparterns.funil.application.dto.MotivoPerdaCriadoDto;
import tech.techparterns.funil.application.dto.MotivoPerdaItemDto;
import tech.techparterns.funil.application.query.ListarMotivosPerdaQuery;
import tech.techparterns.funil.application.usecase.AlterarAtivoMotivoPerdaUseCase;
import tech.techparterns.funil.application.usecase.CadastrarMotivoPerdaUseCase;
import tech.techparterns.funil.application.usecase.ListarMotivosPerdaUseCase;
import tech.techparterns.shared.domain.exception.DomainException;

@Path("/funil/motivos-perda")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Tag(name = "Motivos de perda", description = "Motivos de perda da empresa autenticada.")
public class MotivoPerdaResource {

    private final CadastrarMotivoPerdaUseCase cadastrar;
    private final AlterarAtivoMotivoPerdaUseCase alterarAtivo;
    private final ListarMotivosPerdaUseCase listar;

    public MotivoPerdaResource(CadastrarMotivoPerdaUseCase cadastrar, AlterarAtivoMotivoPerdaUseCase alterarAtivo,
                               ListarMotivosPerdaUseCase listar) {
        this.cadastrar = cadastrar;
        this.alterarAtivo = alterarAtivo;
        this.listar = listar;
    }

    @POST
    @RolesAllowed("ADMIN")
    @Operation(summary = "Cadastrar motivo de perda", description = "Cria um motivo de perda na empresa do token.")
    @APIResponses({
            @APIResponse(responseCode = "201", description = "Motivo criado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = MotivoPerdaCriadoDto.class))),
            @APIResponse(responseCode = "400", description = "Requisição inválida.")
    })
    public Response criar(
            @RequestBody(required = true,
                    content = @Content(schema = @Schema(implementation = CadastrarMotivoPerdaCommand.class)))
            CadastrarMotivoPerdaCommand cmd) {
        var resultado = cadastrar.execute(cmd);
        if (resultado.isFailure())
            throw new DomainException(resultado.getErrors());
        return Response.status(Response.Status.CREATED)
                .entity(new MotivoPerdaCriadoDto(resultado.getValue()))
                .build();
    }

    @PATCH
    @Path("/{id}")
    @RolesAllowed("ADMIN")
    @Operation(summary = "Ativar ou desativar motivo de perda",
            description = "Altera o indicador de ativo do motivo da empresa do token.")
    @APIResponses({
            @APIResponse(responseCode = "204", description = "Indicador de ativo alterado."),
            @APIResponse(responseCode = "400", description = "Requisição inválida.")
    })
    public Response alterar(
            @PathParam("id") UUID id,
            @RequestBody(required = true,
                    content = @Content(schema = @Schema(implementation = AlterarAtivoMotivoPerdaCommand.class)))
            AlterarAtivoMotivoPerdaCommand cmd) {
        var ativo = cmd == null ? null : cmd.ativo();
        var resultado = alterarAtivo.execute(new AlterarAtivoMotivoPerdaCommand(id, ativo));
        if (resultado.isFailure())
            throw new DomainException(resultado.getErrors());
        return Response.noContent().build();
    }

    @GET
    @RolesAllowed({"ADMIN", "SELLER"})
    @Operation(summary = "Listar motivos de perda", description = "Motivos da empresa do token.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Motivos de perda.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(type = SchemaType.ARRAY, implementation = MotivoPerdaItemDto.class)))
    })
    public List<MotivoPerdaItemDto> listar() {
        return listar.execute(new ListarMotivosPerdaQuery()).getValue();
    }
}
