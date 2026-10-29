package tech.techparterns.identidade.infrastructure.security;

import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

import tech.techparterns.identidade.domain.model.HashSenha;
import tech.techparterns.identidade.domain.model.SenhaPlana;
import tech.techparterns.identidade.domain.port.HasherSenha;
import tech.techparterns.identidade.infrastructure.config.IdentidadeConfig;

@ApplicationScoped
public class HasherSenhaBcrypt implements HasherSenha {

    private final IdentidadeConfig config;
    private String hashFicticio;

    public HasherSenhaBcrypt(IdentidadeConfig config) { this.config = config; }

    @PostConstruct
    void iniciar() {
        // Hash de mesmo custo para igualar o tempo quando não há hash real (CA-02).
        hashFicticio = BcryptUtil.bcryptHash("senha-ficticia-1", config.bcryptCost());
    }

    @Override
    public HashSenha hash(SenhaPlana senha) {
        return HashSenha.of(BcryptUtil.bcryptHash(senha.valor(), config.bcryptCost()));
    }

    @Override
    public boolean confere(String senha, HashSenha hash) {
        return senha != null && BcryptUtil.matches(senha, hash.valor());
    }

    @Override
    public void consumirTempoEquivalente(String senha) {
        BcryptUtil.matches(senha == null ? "" : senha, hashFicticio);
    }
}
