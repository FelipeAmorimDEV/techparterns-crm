package tech.techparterns.shared.domain.exception;

/**
 * Exceção de domínio com status HTTP e código próprios (401, 403, 404, 409, 429 e 400 com código).
 * O {@code GlobalExceptionHandler} trata esta classe antes de {@link DomainException}.
 */
public class ErroHttpException extends DomainException {

    private final int status;
    private final String codigo;
    private final Long retryAfterSegundos;

    protected ErroHttpException(int status, String codigo, String mensagem, Long retryAfterSegundos) {
        super(mensagem);
        this.status = status;
        this.codigo = codigo;
        this.retryAfterSegundos = retryAfterSegundos;
    }

    public ErroHttpException(int status, String codigo, String mensagem) {
        this(status, codigo, mensagem, null);
    }

    public int status() { return status; }
    public String codigo() { return codigo; }
    public Long retryAfterSegundos() { return retryAfterSegundos; }
    public String mensagem() { return getErrors().get(0); }
}
