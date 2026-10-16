package tech.techparterns.shared.domain.exception;

public class ConflitoException extends ErroHttpException {
    public ConflitoException(String codigo, String mensagem) {
        super(409, codigo, mensagem);
    }
}
