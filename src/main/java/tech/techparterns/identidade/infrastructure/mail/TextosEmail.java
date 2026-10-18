package tech.techparterns.identidade.infrastructure.mail;

import jakarta.enterprise.context.ApplicationScoped;

import tech.techparterns.identidade.application.service.EmailParaEnviar;
import tech.techparterns.identidade.domain.model.TipoTokenSenha;
import tech.techparterns.identidade.infrastructure.config.IdentidadeConfig;

/** Textos dos dois e-mails (seção 7.9 da spec), em português. */
@ApplicationScoped
public class TextosEmail {

    private final IdentidadeConfig config;

    public TextosEmail(IdentidadeConfig config) { this.config = config; }

    public String assunto(TipoTokenSenha tipo) {
        return tipo == TipoTokenSenha.SETUP
                ? "Defina sua senha de acesso ao TechPartners CRM"
                : "Redefinição de senha do TechPartners CRM";
    }

    public String corpo(EmailParaEnviar e) {
        String base = config.frontendUrl().replaceAll("/+$", "");
        if (e.tipo() == TipoTokenSenha.SETUP) {
            String link = base + config.rotas().definirSenha() + "?token=" + e.tokenPlano();
            return "Olá, " + e.nomeDestinatario() + ".\n\n"
                    + "Você foi cadastrado(a) na empresa " + e.nomeEmpresa() + " no TechPartners CRM.\n"
                    + "Para definir sua senha, acesse o link abaixo (válido por 48 horas):\n\n"
                    + link + "\n\n"
                    + "Se você não esperava este e-mail, ignore esta mensagem.\n";
        }
        String link = base + config.rotas().redefinirSenha() + "?token=" + e.tokenPlano();
        return "Olá, " + e.nomeDestinatario() + ".\n\n"
                + "Recebemos um pedido de redefinição de senha para sua conta (empresa " + e.nomeEmpresa() + ").\n"
                + "Para escolher uma nova senha, acesse o link abaixo (válido por 1 hora):\n\n"
                + link + "\n\n"
                + "Se você não solicitou, ignore este e-mail; sua senha não foi alterada.\n";
    }
}
