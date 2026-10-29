package tech.techparterns.shared.domain.exception;

public class AcessoNegadoException extends ErroHttpException {
    public AcessoNegadoException(String mensagem) {
        super(403, "ACESSO_NEGADO", mensagem);
    }
}
