package tech.techparterns.identidade.infrastructure.rest;

import java.util.List;
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
import tech.techparterns.identidade.application.dto.EmpresaListadaDto;
import tech.techparterns.identidade.application.query.ListarEmpresasQuery;
import tech.techparterns.identidade.application.usecase.*;

@Path("/plataforma/empresas")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("PLATFORM_OWNER")
@ApplicationScoped
@Tag(name = "Plataforma", description = "Gestão de empresas pelo dono do SaaS (PLATFORM_OWNER).")
public class PlataformaEmpresaResource {

    private final CriarEmpresaUseCase criar;
    private final ListarEmpresasUseCase listar;
    private final EditarEmpresaUseCase editar;
    private final SuspenderEmpresaUseCase suspender;
    private final ReativarEmpresaUseCase reativar;
    private final ReenviarSetupAdminUseCase reenviar;
    private final RecuperarEmpresaUseCase recuperar;

    public PlataformaEmpresaResource(CriarEmpresaUseCase criar, ListarEmpresasUseCase listar,
                                     EditarEmpresaUseCase editar, SuspenderEmpresaUseCase suspender,
                                     ReativarEmpresaUseCase reativar, ReenviarSetupAdminUseCase reenviar,
                                     RecuperarEmpresaUseCase recuperar) {
        this.criar = criar;
        this.listar = listar;
        this.editar = editar;
        this.suspender = suspender;
        this.reativar = reativar;
        this.reenviar = reenviar;
        this.recuperar = recuperar;
    }

    @POST
    @Operation(summary = "Criar empresa e primeiro admin")
    @APIResponses({
            @APIResponse(responseCode = "201", description = "Empresa criada."),
            @APIResponse(responseCode = "400", description = "Dados inválidos."),
            @APIResponse(responseCode = "409", description = "E-mail em uso.")
    })
    public Response criar(CriarEmpresaCommand cmd) {
        return Response.status(Response.Status.CREATED).entity(Respostas.valor(criar.execute(cmd))).build();
    }

    @GET
    @Operation(summary = "Listar empresas")
    @APIResponses({@APIResponse(responseCode = "200", description = "Empresas.")})
    public List<EmpresaListadaDto> listar() {
        return Respostas.valor(listar.execute(new ListarEmpresasQuery()));
    }

    @PATCH
    @Path("/{id}")
    @Operation(summary = "Editar o nome da empresa")
    @APIResponses({
            @APIResponse(responseCode = "204", description = "Empresa atualizada."),
            @APIResponse(responseCode = "400", description = "Nome inválido."),
            @APIResponse(responseCode = "404", description = "Empresa não encontrada.")
    })
    public Response editar(@PathParam("id") UUID id, EditarEmpresaCommand cmd) {
        Respostas.valor(editar.execute(new EditarEmpresaCommand(id, cmd.nome())));
        return Response.noContent().build();
    }

    @POST
    @Path("/{id}/suspender")
    @Operation(summary = "Suspender empresa")
    @APIResponses({
            @APIResponse(responseCode = "204", description = "Empresa suspensa."),
            @APIResponse(responseCode = "400", description = "Motivo inválido."),
            @APIResponse(responseCode = "404", description = "Empresa não encontrada."),
            @APIResponse(responseCode = "409", description = "Empresa já suspensa.")
    })
    public Response suspender(@PathParam("id") UUID id, SuspenderEmpresaCommand cmd) {
        String motivo = cmd == null ? null : cmd.motivo();
        Respostas.valor(suspender.execute(new SuspenderEmpresaCommand(id, motivo)));
        return Response.noContent().build();
    }

    @POST
    @Path("/{id}/reativar")
    @Consumes(MediaType.WILDCARD)
    @Operation(summary = "Reativar empresa")
    @APIResponses({
            @APIResponse(responseCode = "204", description = "Empresa reativada."),
            @APIResponse(responseCode = "404", description = "Empresa não encontrada."),
            @APIResponse(responseCode = "409", description = "Empresa não está suspensa.")
    })
    public Response reativar(@PathParam("id") UUID id) {
        Respostas.valor(reativar.execute(new EmpresaIdCommand(id)));
        return Response.noContent().build();
    }

    @POST
    @Path("/{id}/reenviar-definicao-senha-admin")
    @Operation(summary = "Reenviar o link de definição de senha de um admin PENDING")
    @APIResponses({
            @APIResponse(responseCode = "202", description = "Reenvio solicitado."),
            @APIResponse(responseCode = "400", description = "Usuário não está pendente."),
            @APIResponse(responseCode = "404", description = "Empresa ou usuário não encontrado."),
            @APIResponse(responseCode = "409", description = "Empresa suspensa."),
            @APIResponse(responseCode = "429", description = "Intervalo mínimo de 60 s entre envios.")
    })
    public Response reenviar(@PathParam("id") UUID id, ReenviarSetupAdminCommand cmd) {
        UUID usuarioId = cmd == null ? null : cmd.usuarioId();
        Respostas.valor(reenviar.execute(new ReenviarSetupAdminCommand(id, usuarioId)));
        return Response.accepted().build();
    }

    @POST
    @Path("/{id}/recuperar")
    @Operation(summary = "Recuperar empresa sem admin ativo")
    @APIResponses({
            @APIResponse(responseCode = "201", description = "Novo admin PENDING criado."),
            @APIResponse(responseCode = "404", description = "Empresa não encontrada."),
            @APIResponse(responseCode = "409", description = "Empresa suspensa, com admin ativo ou e-mail em uso.")
    })
    public Response recuperar(@PathParam("id") UUID id, RecuperarEmpresaCommand cmd) {
        var resultado = recuperar.execute(new RecuperarEmpresaCommand(id, cmd.adminNome(), cmd.adminEmail(), cmd.adminTelefone()));
        return Response.status(Response.Status.CREATED).entity(Respostas.valor(resultado)).build();
    }
}
