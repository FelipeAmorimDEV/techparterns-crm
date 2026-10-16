package tech.techparterns.shared.domain.exception;

public enum ErroDominio {

    EMPRESA_OBRIGATORIA("Empresa é obrigatória");

    private final String mensagem;

    ErroDominio(String mensagem) {
        this.mensagem = mensagem;
    }

    public String mensagem() {
        return mensagem;
    }

    public String mensagem(Object... args) {
        return String.format(mensagem, args);
    }

    @Override
    public String toString() {
        return mensagem;
    }
}
