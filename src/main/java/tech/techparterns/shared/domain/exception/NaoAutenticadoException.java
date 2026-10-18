package tech.techparterns.shared.domain.exception;

public class NaoAutenticadoException extends ErroHttpException {
    public NaoAutenticadoException(String mensagem) {
        super(401, "NAO_AUTENTICADO", mensagem);
    }
}
