package tech.techparterns.identidade.domain.repository;

import java.time.Instant;

import tech.techparterns.identidade.domain.service.PoliticaBloqueioLogin;

/** Contadores e bloqueios de login, separados das contas (D10). Chaves usam o hash do e-mail digitado. */
public interface ControleLoginRepository {

    /** Há bloqueio ativo para o par, para o e-mail ou para o IP? */
    boolean estaBloqueado(String emailHash, String ip, Instant agora);

    /**
     * Registra uma falha (somente fora de bloqueio) e dispara os bloqueios que a política mandar.
     * @return {@code true} se algum bloqueio foi disparado por esta falha
     */
    boolean registrarFalha(String emailHash, String ip, Instant agora, PoliticaBloqueioLogin politica);

    /** Zera o contador do par (login correto). */
    void zerarPar(String emailHash, String ip);
}
