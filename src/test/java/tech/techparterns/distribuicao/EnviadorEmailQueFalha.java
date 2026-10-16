package tech.techparterns.distribuicao;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;

import tech.techparterns.identidade.domain.port.EnviadorEmail;

@Alternative
@ApplicationScoped
public class EnviadorEmailQueFalha implements EnviadorEmail {

    @Override
    public void enviar(String destinatario, String assunto, String corpo) {
        throw new IllegalStateException("falha definitiva");
    }
}
