package tech.techparterns.shared.seguranca;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.quarkus.security.Authenticated;
import tech.techparterns.identidade.infrastructure.security.ContextoAutenticado;
import tech.techparterns.shared.infrastructure.security.TenantContext;

/** Recurso JAX-RS usado SOMENTE em testes, para verificar o contrato de segurança (R-01). */
@Path("/teste-seguranca")
@Produces(MediaType.TEXT_PLAIN)
public class RecursoSegurancaTeste {

    @Inject
    TenantContext tenant;

    @Inject
    ContextoAutenticado contexto;

    @GET
    @Path("/autenticado")
    @Authenticated
    public String autenticado() {
        return "ok";
    }

    @GET
    @Path("/admin")
    @RolesAllowed("ADMIN")
    public String admin() {
        return "admin";
    }

    @GET
    @Path("/dono")
    @RolesAllowed("PLATFORM_OWNER")
    public String dono() {
        return "dono";
    }

    @GET
    @Path("/papel")
    @Authenticated
    public String papel() {
        return tenant.role();
    }

    @GET
    @Path("/empresa")
    @Authenticated
    public String empresa() {
        return contexto.empresaId().toString();
    }
}
