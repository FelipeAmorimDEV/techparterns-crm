package tech.techparterns.identidade.domain.port;

public interface ContextoRequisicao {

    /** IP do cliente da requisição corrente (ver DTP-5 sobre proxy confiável). */
    String ip();
}
