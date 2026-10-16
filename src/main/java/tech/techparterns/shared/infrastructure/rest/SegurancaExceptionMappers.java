package tech.techparterns.shared.infrastructure.rest;

import java.time.OffsetDateTime;

import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.ForbiddenException;
import io.quarkus.security.UnauthorizedException;
import jakarta.annotation.Priority;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Os mapeadores embutidos do Quarkus já devolvem 401 e 403, mas com corpo vazio.
 * Estes mantêm os status e padronizam o corpo com {@link ErroResponse} (R-01, CA-27).
 */
public final class SegurancaExceptionMappers {

    private SegurancaExceptionMappers() {}

    static Response resposta(UriInfo uriInfo, int status, String mensagem, String codigo) {
        String path = "/";
        if (uriInfo != null) {
            String raw = uriInfo.getPath();
            path = raw.startsWith("/") ? raw : "/" + raw;
        }
        return Response.status(status).type(MediaType.APPLICATION_JSON)
                .entity(new ErroResponse(path, mensagem, codigo, OffsetDateTime.now())).build();
    }

    @Provider
    @Priority(1)
    public static class NaoAutorizado implements ExceptionMapper<UnauthorizedException> {
        @Context UriInfo uriInfo;

        @Override
        public Response toResponse(UnauthorizedException e) {
            return resposta(uriInfo, 401, "Autenticação necessária", "NAO_AUTENTICADO");
        }
    }

    @Provider
    @Priority(1)
    public static class FalhaAutenticacao implements ExceptionMapper<AuthenticationFailedException> {
        @Context UriInfo uriInfo;

        @Override
        public Response toResponse(AuthenticationFailedException e) {
            return resposta(uriInfo, 401, "Autenticação necessária", "NAO_AUTENTICADO");
        }
    }

    @Provider
    @Priority(1)
    public static class Proibido implements ExceptionMapper<ForbiddenException> {
        @Context UriInfo uriInfo;

        @Override
        public Response toResponse(ForbiddenException e) {
            return resposta(uriInfo, 403, "Acesso negado", "ACESSO_NEGADO");
        }
    }
}
