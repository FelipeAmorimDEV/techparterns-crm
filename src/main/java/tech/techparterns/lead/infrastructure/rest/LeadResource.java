package tech.techparterns.lead.infrastructure.rest;

import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import tech.techparterns.distribuicao.application.Contratos;
import tech.techparterns.distribuicao.application.EditarFatosLead;
import tech.techparterns.lead.application.command.ExcluirLeadCommand;
import tech.techparterns.lead.application.command.ImportarLeadsCommand;
import tech.techparterns.lead.application.dto.ImportacaoLeadDto;
import tech.techparterns.lead.application.usecase.ExcluirLeadUseCase;
import tech.techparterns.lead.application.usecase.ImportarLeadsUseCase;
import tech.techparterns.shared.domain.exception.DomainException;

@Path("/leads")
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Tag(name = "Leads", description = "Leads da empresa autenticada.")
public class LeadResource {

    private final ImportarLeadsUseCase importarLeads;
    private final ExcluirLeadUseCase excluirLead;
    private final EditarFatosLead editarFatos;

    public LeadResource(ImportarLeadsUseCase importarLeads, ExcluirLeadUseCase excluirLead, EditarFatosLead editarFatos) {
        this.importarLeads = importarLeads;
        this.excluirLead = excluirLead;
        this.editarFatos = editarFatos;
    }

    @POST
    @Path("/importacoes")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Operation(summary = "Importar leads por CSV",
            description = "Campos multipart: arquivo (CSV UTF-8) e acao (ignorar, completar_campos_vazios ou criar_mesmo_assim).")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Arquivo processado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = ImportacaoLeadDto.class))),
            @APIResponse(responseCode = "400", description = "Requisição inválida.")
    })
    public ImportacaoLeadDto importar(@RestForm("arquivo") FileUpload arquivo, @RestForm("acao") String acao)
            throws IOException {
        byte[] bytes = arquivo == null ? null : Files.readAllBytes(arquivo.uploadedFile());
        var resultado = importarLeads.execute(new ImportarLeadsCommand(bytes, acao));
        if (resultado.isFailure())
            throw new DomainException(resultado.getErrors());
        return resultado.getValue();
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Excluir lead", description = "Exclusão lógica do lead da empresa do token.")
    @APIResponses({
            @APIResponse(responseCode = "204", description = "Lead excluído."),
            @APIResponse(responseCode = "400", description = "Requisição inválida.")
    })
    public Response excluir(@PathParam("id") UUID id) {
        var resultado = excluirLead.execute(new ExcluirLeadCommand(id));
        if (resultado.isFailure())
            throw new DomainException(resultado.getErrors());
        return Response.noContent().build();
    }

    @PATCH
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed({"ADMIN", "SELLER"})
    public Response editar(@PathParam("id") UUID id, Contratos.Fatos fatos) {
        var resultado = editarFatos.editar(id, fatos);
        if (resultado.isFailure())
            throw new DomainException(resultado.getErrors());
        return Response.noContent().build();
    }
}
