package tech.techparterns.identidade.infrastructure.rest;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import tech.techparterns.identidade.application.command.EditarPerfilCommand;
import tech.techparterns.identidade.application.command.TrocarSenhaCommand;
import tech.techparterns.identidade.application.dto.PerfilDto;
import tech.techparterns.identidade.application.query.ConsultarPerfilQuery;
import tech.techparterns.identidade.application.usecase.ConsultarPerfilUseCase;
import tech.techparterns.identidade.application.usecase.EditarPerfilUseCase;
import tech.techparterns.identidade.application.usecase.TrocarSenhaUseCase;

@Path("/me")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Tag(name = "Perfil", description = "Perfil e senha do usuário de empresa autenticado.")
public class PerfilResource {

    private final ConsultarPerfilUseCase consultar;
    private final EditarPerfilUseCase editar;
    private final TrocarSenhaUseCase trocarSenha;

    public PerfilResource(ConsultarPerfilUseCase consultar, EditarPerfilUseCase editar, TrocarSenhaUseCase trocarSenha) {
        this.consultar = consultar;
        this.editar = editar;
        this.trocarSenha = trocarSenha;
    }

    @GET
    @RolesAllowed({"ADMIN", "SELLER"})
    @Operation(summary = "Consultar o próprio perfil")
    @APIResponses({@APIResponse(responseCode = "200", description = "Perfil.")})
    public PerfilDto consultar() {
        return Respostas.valor(consultar.execute(new ConsultarPerfilQuery()));
    }

    @PATCH
    @RolesAllowed({"ADMIN", "SELLER"})
    @Operation(summary = "Editar o próprio nome e telefone", description = "O próprio e-mail não é alterável.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Perfil atualizado."),
            @APIResponse(responseCode = "400", description = "Dados inválidos ou tentativa de alterar o e-mail.")
    })
    public PerfilDto editar(EditarPerfilCommand cmd) {
        return Respostas.valor(editar.execute(cmd));
    }

    @PUT
    @Path("/senha")
    @RolesAllowed({"ADMIN", "SELLER"})
    @Operation(summary = "Trocar a própria senha", description = "Exige a senha atual; revoga todas as sessões.")
    @APIResponses({
            @APIResponse(responseCode = "204", description = "Senha alterada."),
            @APIResponse(responseCode = "400", description = "Senha atual incorreta ou nova senha inválida.")
    })
    public Response trocarSenha(TrocarSenhaCommand cmd) {
        Respostas.valor(trocarSenha.execute(cmd));
        return Response.noContent().build();
    }
}
