package tech.techparterns.shared.infrastructure.rest;

import java.time.OffsetDateTime;


import tech.techparterns.shared.domain.exception.DomainException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.annotation.Priority;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class GlobalExceptionHandler implements ExceptionMapper<Throwable> {

    private static final String ERRO_INTERNO_MENSAGEM = "Erro interno no servidor";

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(Throwable exception) {
        var erroMapeado = mapear(exception);

        return Response.status(erroMapeado.status())
                .type(MediaType.APPLICATION_JSON)
                .entity(new ErroResponse(
                        path(),
                        erroMapeado.mensagem(),
                        erroMapeado.codigoErro(),
                        OffsetDateTime.now()))
                .build();
    }

    private ErroMapeado mapear(Throwable exception) {
        if (exception instanceof NotFoundException) {
            return new ErroMapeado(Response.Status.NOT_FOUND, "Rota não encontrada", "ROTA_NAO_ENCONTRADA");
        }
        if (exception instanceof DomainException de) {
            String mensagem = de.getErrors().size() == 1
                    ? de.getErrors().get(0)
                    : String.join("; ", de.getErrors());
            return new ErroMapeado(Response.Status.BAD_REQUEST, mensagem, "REQUISICAO_INVALIDA");
        }
        if (exception instanceof IllegalArgumentException) {
            return new ErroMapeado(Response.Status.BAD_REQUEST, exception.getMessage(), "REQUISICAO_INVALIDA");
        }
        if (exception instanceof IllegalStateException
                && exception.getMessage() != null
                && exception.getMessage().contains("Tenant")) {
            return new ErroMapeado(Response.Status.UNAUTHORIZED, exception.getMessage(), "TENANT_AUSENTE");
        }
        var causa = exception;
        while (causa != null) {
            if (causa instanceof InvalidFormatException) {
                return new ErroMapeado(Response.Status.BAD_REQUEST, "Requisição inválida", "REQUISICAO_INVALIDA");
            }
            causa = causa.getCause();
        }
        return new ErroMapeado(Response.Status.INTERNAL_SERVER_ERROR, ERRO_INTERNO_MENSAGEM, "ERRO_INTERNO");
    }

    private String path() {
        if (uriInfo == null) {
            return "/";
        }
        String path = uriInfo.getPath();
        return path.startsWith("/") ? path : "/" + path;
    }

    private record ErroMapeado(Response.Status status, String mensagem, String codigoErro) {
    }
}

@Provider
@Priority(Priorities.USER)
class InvalidFormatExceptionHandler implements ExceptionMapper<InvalidFormatException> {

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(InvalidFormatException exception) {
        String path = "/";
        if (uriInfo != null) {
            String raw = uriInfo.getPath();
            path = raw.startsWith("/") ? raw : "/" + raw;
        }
        return Response.status(Response.Status.BAD_REQUEST)
                .type(MediaType.APPLICATION_JSON)
                .entity(new ErroResponse(path, "Requisição inválida", "REQUISICAO_INVALIDA", OffsetDateTime.now()))
                .build();
    }
}
