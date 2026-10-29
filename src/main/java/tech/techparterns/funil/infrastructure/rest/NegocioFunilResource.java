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
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import tech.techparterns.funil.application.command.AtualizarDadosAbertosCommand;
import tech.techparterns.funil.application.dto.DadosAbertosDto;
import tech.techparterns.funil.application.usecase.AtualizarDadosAbertosUseCase;
import tech.techparterns.shared.domain.exception.DomainException;

@Path("/funil/leads")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Tag(name = "Negócio do funil", description = "Dados comerciais do lead aberto na empresa autenticada.")
public class NegocioFunilResource {

    private final AtualizarDadosAbertosUseCase atualizarDadosAbertos;

    public NegocioFunilResource(AtualizarDadosAbertosUseCase atualizarDadosAbertos) {
        this.atualizarDadosAbertos = atualizarDadosAbertos;
    }

    @PATCH
    @Path("/{leadId}")
    @RolesAllowed({"ADMIN", "SELLER"})
    @Operation(summary = "Atualizar dados abertos",
            description = "Grava roteiro, demo e dados comerciais do lead aberto, sem mudar a etapa.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Dados abertos gravados.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = DadosAbertosDto.class))),
            @APIResponse(responseCode = "400", description = "Requisição inválida.")
    })
    public Response atualizar(
            @PathParam("leadId") UUID leadId,
            @RequestBody(required = true,
                    content = @Content(schema = @Schema(implementation = AtualizarDadosAbertosCommand.class)))
            AtualizarDadosAbertosCommand cmd) {
        var corpo = cmd == null
                ? new AtualizarDadosAbertosCommand(leadId, null, null, null, null, null, null, null, null, null)
                : new AtualizarDadosAbertosCommand(
                        leadId, cmd.decisor(), cmd.dorPrincipal(), cmd.usuariosPrevistos(), cmd.prazoDecisao(),
                        cmd.orcamento(), cmd.dataDemo(), cmd.valorEstimado(), cmd.planoInteresse(),
                        cmd.previsaoFechamento());
        var resultado = atualizarDadosAbertos.execute(corpo);
        if (resultado.isFailure())
            throw new DomainException(resultado.getErrors());
        return Response.ok(resultado.getValue()).build();
    }
}
