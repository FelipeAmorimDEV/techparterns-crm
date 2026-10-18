package tech.techparterns.identidade.domain.port;

import tech.techparterns.identidade.domain.model.HashSenha;
import tech.techparterns.identidade.domain.model.SenhaPlana;

public interface HasherSenha {

    HashSenha hash(SenhaPlana senha);

    boolean confere(String senha, HashSenha hash);

    /** Executa uma verificação de custo equivalente quando não há hash real (iguala o tempo de resposta). */
    void consumirTempoEquivalente(String senha);
}
