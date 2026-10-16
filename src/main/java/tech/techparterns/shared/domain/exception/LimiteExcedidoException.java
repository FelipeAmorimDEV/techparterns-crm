package tech.techparterns.shared.domain.exception;

public class LimiteExcedidoException extends ErroHttpException {
    public LimiteExcedidoException(String mensagem, long retryAfterSegundos) {
        super(429, "LIMITE_EXCEDIDO", mensagem, Math.max(1, retryAfterSegundos));
    }
}
