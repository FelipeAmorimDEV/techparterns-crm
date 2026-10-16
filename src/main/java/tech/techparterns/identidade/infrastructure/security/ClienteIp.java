package tech.techparterns.identidade.infrastructure.security;

import io.vertx.core.http.HttpServerRequest;

/**
 * IP do cliente (DTP-5): lê o endereço remoto da conexão. Atrás de proxy, o Quarkus só o reescreve a partir de
 * X-Forwarded-For quando {@code quarkus.http.proxy.proxy-address-forwarding} está ligado e o proxy é confiável.
 */
public final class ClienteIp {

    private ClienteIp() {}

    public static String de(HttpServerRequest request) {
        if (request == null || request.remoteAddress() == null)
            return "desconhecido";
        String ip = request.remoteAddress().hostAddress();
        return ip == null ? "desconhecido" : (ip.length() > 45 ? ip.substring(0, 45) : ip);
    }
}
