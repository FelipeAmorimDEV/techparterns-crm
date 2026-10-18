package tech.techparterns.identidade.domain.service;

import java.util.Collection;
import java.util.UUID;

import tech.techparterns.identidade.domain.model.UsuarioEmpresa;

/** RN-IA-10 / DEL-07: só ADMIN ACTIVE conta como "outro admin"; PENDING não conta. */
public final class PoliticaUltimoAdmin {

    private PoliticaUltimoAdmin() {}

    /**
     * Pode-se desativar ou rebaixar {@code alvoId}? Se o alvo não é um ADMIN ACTIVE a restrição não se
     * aplica; caso contrário precisa existir outro ADMIN ACTIVE na empresa.
     */
    public static boolean podeRemoverAdminAtivo(Collection<UsuarioEmpresa> adminsDaEmpresa, UUID alvoId) {
        boolean alvoEhAdminAtivo = adminsDaEmpresa.stream()
                .anyMatch(u -> u.getId().equals(alvoId) && u.ehAdminAtivo());
        if (!alvoEhAdminAtivo)
            return true;
        return adminsDaEmpresa.stream().anyMatch(u -> !u.getId().equals(alvoId) && u.ehAdminAtivo());
    }
}
