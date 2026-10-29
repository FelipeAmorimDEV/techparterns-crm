package tech.techparterns.identidade.infrastructure.mail;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import jakarta.enterprise.context.ApplicationScoped;

import tech.techparterns.identidade.domain.port.EnviadorEmail;

@ApplicationScoped
public class EnviadorEmailMailer implements EnviadorEmail {

    private final Mailer mailer;

    public EnviadorEmailMailer(Mailer mailer) { this.mailer = mailer; }

    @Override
    public void enviar(String destinatario, String assunto, String corpo) {
        mailer.send(Mail.withText(destinatario, assunto, corpo));
    }
}
