package tech.techparterns.identidade.infrastructure.rest;

import java.util.UUID;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import tech.techparterns.identidade.application.command.*;
import tech.techparterns.identidade.application.dto.PaginaDto;
import tech.techparterns.identidade.application.dto.UsuarioListadoDto;
import tech.techparterns.identidade.application.query.ListarUsuariosQuery;
import tech.techparterns.identidade.application.usecase.*;

@Path("/usuarios")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("ADMIN")
@ApplicationScoped
@Tag(name = "Usuários", description = "Gestão de usuários da própria empresa (somente ADMIN).")
public class UsuarioResource {

    private final CadastrarUsuarioUseCase cadastrar;
    private final ListarUsuariosUseCase listar;
    private final EditarUsuarioUseCase editar;
    private final DesativarUsuarioUseCase desativar;
    private final ReativarUsuarioUseCase reativar;
    private final AlterarPapelUsuarioUseCase alterarPapel;
    private final ReenviarDefinicaoSenhaUseCase reenviar;

    public UsuarioResource(CadastrarUsuarioUseCase cadastrar, ListarUsuariosUseCase listar,
                           EditarUsuarioUseCase editar, DesativarUsuarioUseCase desativar,
                           ReativarUsuarioUseCase reativar, AlterarPapelUsuarioUseCase alterarPapel,
                           ReenviarDefinicaoSenhaUseCase reenviar) {
        this.cadastrar = cadastrar;
        this.listar = listar;
        this.editar = editar;
        this.desativar = desativar;
        this.reativar = reativar;
        this.alterarPapel = alterarPapel;
        this.reenviar = reenviar;
    }

    @POST
    @Operation(summary = "Cadastrar usuário", description = "Resposta neutra 202, exista ou não o e-mail.")
    @APIResponses({
            @APIResponse(responseCode = "202", description = "Solicitação recebida."),
            @APIResponse(responseCode = "400", description = "Dados inválidos."),
            @APIResponse(responseCode = "429", description = "Limite por hora excedido.")
    })
    public Response cadastrar(CadastrarUsuarioCommand cmd) {
        return Response.accepted(Respostas.valor(cadastrar.execute(cmd))).build();
    }

    @GET
    @Operation(summary = "Listar usuários da empresa")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Página de usuários."),
            @APIResponse(responseCode = "400", description = "Paginação ou filtro inválido.")
    })
    public PaginaDto<UsuarioListadoDto> listar(@QueryParam("page") Integer page, @QueryParam("size") Integer size,
                                               @QueryParam("status") String status, @QueryParam("role") String role) {
        return Respostas.valor(listar.execute(new ListarUsuariosQuery(page, size, status, role)));
    }

    @PATCH
    @Path("/{id}")
    @Operation(summary = "Editar nome, e-mail e telefone de outro usuário")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Usuário atualizado."),
            @APIResponse(responseCode = "400", description = "Dados inválidos ou e-mail indisponível."),
            @APIResponse(responseCode = "404", description = "Usuário não encontrado.")
    })
    public UsuarioListadoDto editar(@PathParam("id") UUID id, EditarUsuarioCommand cmd) {
        return Respostas.valor(editar.execute(new EditarUsuarioCommand(id, cmd.nome(), cmd.email(), cmd.telefone())));
    }

    @POST
    @Path("/{id}/desativar")
    @Consumes(MediaType.WILDCARD)
    @Operation(summary = "Desativar usuário")
    @APIResponses({
            @APIResponse(responseCode = "204", description = "Usuário desativado."),
            @APIResponse(responseCode = "400", description = "Último admin ativo da empresa."),
            @APIResponse(responseCode = "404", description = "Usuário não encontrado.")
    })
    public Response desativar(@PathParam("id") UUID id) {
        Respostas.valor(desativar.execute(new UsuarioIdCommand(id)));
        return Response.noContent().build();
    }

    @POST
    @Path("/{id}/reativar")
    @Consumes(MediaType.WILDCARD)
    @Operation(summary = "Reativar usuário")
    @APIResponses({
            @APIResponse(responseCode = "204", description = "Usuário reativado."),
            @APIResponse(responseCode = "404", description = "Usuário não encontrado.")
    })
    public Response reativar(@PathParam("id") UUID id) {
        Respostas.valor(reativar.execute(new UsuarioIdCommand(id)));
        return Response.noContent().build();
    }

    @PUT
    @Path("/{id}/papel")
    @Operation(summary = "Promover ou rebaixar usuário")
    @APIResponses({
            @APIResponse(responseCode = "204", description = "Papel alterado."),
            @APIResponse(responseCode = "400", description = "Papel inválido ou último admin ativo."),
            @APIResponse(responseCode = "404", description = "Usuário não encontrado.")
    })
    public Response alterarPapel(@PathParam("id") UUID id, AlterarPapelCommand cmd) {
        Respostas.valor(alterarPapel.execute(new AlterarPapelCommand(id, cmd.role())));
        return Response.noContent().build();
    }

    @POST
    @Path("/{id}/reenviar-definicao-senha")
    @Consumes(MediaType.WILDCARD)
    @Operation(summary = "Reenviar o e-mail de definição de senha de usuário PENDING")
    @APIResponses({
            @APIResponse(responseCode = "202", description = "Reenvio solicitado."),
            @APIResponse(responseCode = "400", description = "Usuário não está pendente."),
            @APIResponse(responseCode = "404", description = "Usuário não encontrado."),
            @APIResponse(responseCode = "429", description = "Intervalo mínimo de 60 s entre envios.")
    })
    public Response reenviar(@PathParam("id") UUID id) {
        Respostas.valor(reenviar.execute(new UsuarioIdCommand(id)));
        return Response.accepted().build();
    }
}
