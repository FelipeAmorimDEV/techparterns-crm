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
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import tech.techparterns.funil.application.command.AtualizarDadosAbertosCommand;
import tech.techparterns.funil.application.command.MarcarGanhoCommand;
import tech.techparterns.funil.application.command.MarcarPerdidoCommand;
import tech.techparterns.funil.application.command.MoverLeadEtapaCommand;
import tech.techparterns.funil.application.dto.DadosAbertosDto;
import tech.techparterns.funil.application.usecase.AtualizarDadosAbertosUseCase;
import tech.techparterns.funil.application.usecase.MarcarGanhoUseCase;
import tech.techparterns.funil.application.usecase.MarcarPerdidoUseCase;
import tech.techparterns.funil.application.usecase.MoverLeadEtapaUseCase;
import tech.techparterns.shared.domain.exception.DomainException;

@Path("/funil/leads")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Tag(name = "Negócio do funil", description = "Dados comerciais do lead aberto na empresa autenticada.")
public class NegocioFunilResource {

    private final AtualizarDadosAbertosUseCase atualizarDadosAbertos;
    private final MoverLeadEtapaUseCase moverLeadEtapa;
    private final MarcarGanhoUseCase marcarGanho;
    private final MarcarPerdidoUseCase marcarPerdido;

    public NegocioFunilResource(AtualizarDadosAbertosUseCase atualizarDadosAbertos,
                                MoverLeadEtapaUseCase moverLeadEtapa,
                                MarcarGanhoUseCase marcarGanho,
                                MarcarPerdidoUseCase marcarPerdido) {
        this.atualizarDadosAbertos = atualizarDadosAbertos;
        this.moverLeadEtapa = moverLeadEtapa;
        this.marcarGanho = marcarGanho;
        this.marcarPerdido = marcarPerdido;
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

    @POST
    @Path("/{leadId}/mover")
    @RolesAllowed({"ADMIN", "SELLER"})
    @Operation(summary = "Mover lead entre etapas abertas",
            description = "Move o lead para outra etapa ABERTO ativa quando a flag do destino está satisfeita.")
    @APIResponses({
            @APIResponse(responseCode = "204", description = "Movimento concluído ou o lead já estava na etapa."),
            @APIResponse(responseCode = "400", description = "Requisição inválida.")
    })
    public Response mover(
            @PathParam("leadId") UUID leadId,
            @RequestBody(required = true,
                    content = @Content(schema = @Schema(implementation = MoverLeadEtapaCommand.class)))
            MoverLeadEtapaCommand cmd) {
        var corpo = new MoverLeadEtapaCommand(leadId, cmd == null ? null : cmd.etapaId());
        var resultado = moverLeadEtapa.execute(corpo);
        if (resultado.isFailure())
            throw new DomainException(resultado.getErrors());
        return Response.noContent().build();
    }

    @POST
    @Path("/{leadId}/ganho")
    @RolesAllowed({"ADMIN", "SELLER"})
    @Operation(summary = "Marcar lead como ganho",
            description = "Coloca o lead na etapa GANHO ativa e grava a situação GANHO.")
    @APIResponses({
            @APIResponse(responseCode = "204", description = "Ganho concluído."),
            @APIResponse(responseCode = "400", description = "Requisição inválida.")
    })
    public Response ganho(
            @PathParam("leadId") UUID leadId,
            @RequestBody(required = true,
                    content = @Content(schema = @Schema(implementation = MarcarGanhoCommand.class)))
            MarcarGanhoCommand cmd) {
        var corpo = new MarcarGanhoCommand(
                leadId,
                cmd == null ? null : cmd.planoFechado(),
                cmd == null ? null : cmd.valorFechado());
        var resultado = marcarGanho.execute(corpo);
        if (resultado.isFailure())
            throw new DomainException(resultado.getErrors());
        return Response.noContent().build();
    }

    @POST
    @Path("/{leadId}/perdido")
    @RolesAllowed({"ADMIN", "SELLER"})
    @Operation(summary = "Marcar lead como perdido",
            description = "Coloca o lead na etapa PERDIDO ativa e grava a situação PERDIDO.")
    @APIResponses({
            @APIResponse(responseCode = "204", description = "Perdido concluído."),
            @APIResponse(responseCode = "400", description = "Requisição inválida.")
    })
    public Response perdido(
            @PathParam("leadId") UUID leadId,
            @RequestBody(required = true,
                    content = @Content(schema = @Schema(implementation = MarcarPerdidoCommand.class)))
            MarcarPerdidoCommand cmd) {
        var corpo = new MarcarPerdidoCommand(
                leadId,
                cmd == null ? null : cmd.motivoPerdaId(),
                cmd == null ? null : cmd.observacao());
        var resultado = marcarPerdido.execute(corpo);
        if (resultado.isFailure())
            throw new DomainException(resultado.getErrors());
        return Response.noContent().build();
    }
}
