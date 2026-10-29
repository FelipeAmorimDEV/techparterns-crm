package tech.techparterns.shared.domain.exception;

public enum ErroDominio {

    EMPRESA_OBRIGATORIA("Empresa é obrigatória"),
    OPERACAO_RESTRITA_AO_ADMIN("Operação restrita ao admin"),
    LEAD_NAO_ENCONTRADO("Lead não encontrado");

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
