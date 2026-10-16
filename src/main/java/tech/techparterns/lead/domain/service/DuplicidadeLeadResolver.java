package tech.techparterns.lead.domain.service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import tech.techparterns.lead.domain.model.EmailContato;
import tech.techparterns.lead.domain.model.Lead;
import tech.techparterns.lead.domain.model.TelefoneBrasil;

public final class DuplicidadeLeadResolver {

    private static final Comparator<Lead> PRECEDENCIA = Comparator
            .comparing(Lead::getUpdatedAt).reversed()
            .thenComparing(Lead::getCreatedAt);

    private DuplicidadeLeadResolver() {}

    /** Telefone primeiro; só sem candidato por telefone cai para o e-mail. Chave ausente não casa. */
    public static Optional<Lead> resolver(TelefoneBrasil telefone, EmailContato email, List<Lead> ativos) {
        if (telefone != null) {
            var porTelefone = escolher(ativos, l -> l.telefone() != null && l.telefone().equals(telefone));
            if (porTelefone.isPresent())
                return porTelefone;
        }
        if (email != null)
            return escolher(ativos, l -> l.email() != null && l.email().equals(email));
        return Optional.empty();
    }

    private static Optional<Lead> escolher(List<Lead> ativos, Predicate<Lead> chave) {
        return ativos.stream()
                .filter(l -> !l.isDeleted())
                .filter(chave)
                .min(PRECEDENCIA);
    }
}
