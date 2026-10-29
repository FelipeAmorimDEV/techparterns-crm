package tech.techparterns.identidade.infrastructure.security;

import java.io.IOException;
import java.time.OffsetDateTime;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import tech.techparterns.identidade.domain.model.ErroIdentidade;
import tech.techparterns.identidade.infrastructure.config.IdentidadeConfig;
import tech.techparterns.shared.infrastructure.rest.ErroResponse;

@Provider
@OrigemPermitida
@Priority(Priorities.AUTHENTICATION)
public class OrigemPermitidaFilter implements ContainerRequestFilter {

    private final jakarta.enterprise.inject.Instance<IdentidadeConfig> config;

    public OrigemPermitidaFilter(jakarta.enterprise.inject.Instance<IdentidadeConfig> config) { this.config = config; }

    @Override
    public void filter(ContainerRequestContext ctx) throws IOException {
        String origem = ctx.getHeaderString("Origin");
        if (origem == null || !config.get().allowedOrigins().contains(origem)) {
            String path = "/" + ctx.getUriInfo().getPath().replaceFirst("^/", "");
            ctx.abortWith(Response.status(403).type(MediaType.APPLICATION_JSON)
                    .entity(new ErroResponse(path, ErroIdentidade.ORIGEM_NAO_PERMITIDA.mensagem(),
                            "ORIGEM_NAO_PERMITIDA", OffsetDateTime.now()))
                    .build());
        }
    }
}
