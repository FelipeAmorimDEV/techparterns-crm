package tech.techparterns.identidade.domain.port;

public interface EnviadorEmail {

    /** Envia ou lança exceção. O corpo e o link nunca são persistidos. */
    void enviar(String destinatario, String assunto, String corpo);
}
