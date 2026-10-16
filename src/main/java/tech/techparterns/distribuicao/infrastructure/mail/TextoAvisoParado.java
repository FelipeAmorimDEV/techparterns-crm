package tech.techparterns.distribuicao.infrastructure.mail;

/**
 * Texto provisório. Assunto e corpo não são decisão de produto aprovada.
 */
public final class TextoAvisoParado {

    public static final String ASSUNTO = "Lead voltou para a fila";

    private TextoAvisoParado() {}

    public static String corpo(String nomeContato) {
        return "O lead " + nomeContato + " voltou para a fila.";
    }
}
