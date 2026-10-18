package tech.techparterns.shared.infrastructure.rest;

import java.time.OffsetDateTime;


import tech.techparterns.shared.domain.exception.DomainException;
import tech.techparterns.shared.domain.exception.ErroHttpException;
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

    private static final org.jboss.logging.Logger LOG = org.jboss.logging.Logger.getLogger(GlobalExceptionHandler.class);
    private static final String ERRO_INTERNO_MENSAGEM = "Erro interno no servidor";

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(Throwable exception) {
        var erroMapeado = mapear(exception);

        var resposta = Response.status(erroMapeado.status())
                .type(MediaType.APPLICATION_JSON)
                .entity(new ErroResponse(
                        path(),
                        erroMapeado.mensagem(),
                        erroMapeado.codigoErro(),
                        OffsetDateTime.now()));
        if (erroMapeado.retryAfter() != null) {
            resposta.header("Retry-After", erroMapeado.retryAfter());
        }
        return resposta.build();
    }

    private ErroMapeado mapear(Throwable exception) {
        if (exception instanceof NotFoundException) {
            return new ErroMapeado(404, "Rota não encontrada", "ROTA_NAO_ENCONTRADA", null);
        }
        if (exception instanceof jakarta.ws.rs.NotSupportedException) {
            return new ErroMapeado(415, "Tipo de conteúdo não suportado", "REQUISICAO_INVALIDA", null);
        }
        if (exception instanceof ErroHttpException eh) {
            return new ErroMapeado(eh.status(), eh.mensagem(), eh.codigo(), eh.retryAfterSegundos());
        }
        if (exception instanceof DomainException de) {
            String mensagem = de.getErrors().size() == 1
                    ? de.getErrors().get(0)
                    : String.join("; ", de.getErrors());
            return new ErroMapeado(400, mensagem, "REQUISICAO_INVALIDA", null);
        }
        if (exception instanceof IllegalArgumentException) {
            return new ErroMapeado(400, exception.getMessage(), "REQUISICAO_INVALIDA", null);
        }
        // TODO: requireCompanyId() lança "Company ausente no token", então esta
        // checagem por "Tenant" não cobre mais empresa ausente. Ajustar a mensagem
        // ou o critério antes de voltar a mapear TENANT_AUSENTE.
        // if (exception instanceof IllegalStateException
        //         && exception.getMessage() != null
        //         && exception.getMessage().contains("Tenant")) {
        //     return new ErroMapeado(Response.Status.UNAUTHORIZED, exception.getMessage(), "TENANT_AUSENTE");
        // }
        var causa = exception;
        while (causa != null) {
            if (causa instanceof java.sql.SQLException sql && "23505".equals(sql.getSQLState())) {
                return new ErroMapeado(409, "Registro já existente", "CONFLITO", null);
            }
            causa = causa.getCause();
        }
        causa = exception;
        while (causa != null) {
            if (causa instanceof InvalidFormatException) {
                return new ErroMapeado(400, "Requisição inválida", "REQUISICAO_INVALIDA", null);
            }
            causa = causa.getCause();
        }
        LOG.error("Erro interno não tratado", exception);
        return new ErroMapeado(500, ERRO_INTERNO_MENSAGEM, "ERRO_INTERNO", null);
    }

    private String path() {
        if (uriInfo == null) {
            return "/";
        }
        String path = uriInfo.getPath();
        return path.startsWith("/") ? path : "/" + path;
    }

    private record ErroMapeado(int status, String mensagem, String codigoErro, Long retryAfter) {
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
