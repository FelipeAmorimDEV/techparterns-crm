package tech.techparterns.identidade.domain.repository;

import tech.techparterns.identidade.domain.model.EmailUsuario;

/** Unicidade global de e-mail (RN-IA-02): considera {@code app_user} e {@code platform_user}. */
public interface EmailRegistradoRepository {

    boolean emUso(EmailUsuario email);
}
