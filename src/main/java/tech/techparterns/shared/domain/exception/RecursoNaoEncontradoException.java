package tech.techparterns.shared.domain.exception;

public class RecursoNaoEncontradoException extends ErroHttpException {
    public RecursoNaoEncontradoException(String mensagem) {
        super(404, "RECURSO_NAO_ENCONTRADO", mensagem);
    }
}
