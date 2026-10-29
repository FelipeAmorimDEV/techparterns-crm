package tech.techparterns.identidade.domain.service;

import java.util.Collection;
import java.util.Optional;

import tech.techparterns.identidade.domain.model.Empresa;
import tech.techparterns.identidade.domain.model.UsuarioEmpresa;

/** DEL-12: só empresa ACTIVE e sem nenhum ADMIN ACTIVE pode ser recuperada. */
public final class PoliticaRecuperacaoEmpresa {

    public enum Recusa { EMPRESA_SUSPENSA, EMPRESA_COM_ADMIN_ATIVO }

    private PoliticaRecuperacaoEmpresa() {}

    public static Optional<Recusa> avaliar(Empresa empresa, Collection<UsuarioEmpresa> admins) {
        if (!empresa.estaAtiva())
            return Optional.of(Recusa.EMPRESA_SUSPENSA);
        if (admins.stream().anyMatch(UsuarioEmpresa::ehAdminAtivo))
            return Optional.of(Recusa.EMPRESA_COM_ADMIN_ATIVO);
        return Optional.empty();
    }
}
