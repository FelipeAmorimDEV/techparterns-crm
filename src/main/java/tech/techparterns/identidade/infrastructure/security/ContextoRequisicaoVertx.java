package tech.techparterns.identidade.infrastructure.security;

import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.RequestScoped;

import tech.techparterns.identidade.domain.port.ContextoRequisicao;

@RequestScoped
public class ContextoRequisicaoVertx implements ContextoRequisicao {

    private final RoutingContext routingContext;

    public ContextoRequisicaoVertx(RoutingContext routingContext) { this.routingContext = routingContext; }

    @Override
    public String ip() {
        return ClienteIp.de(routingContext.request());
    }
}
