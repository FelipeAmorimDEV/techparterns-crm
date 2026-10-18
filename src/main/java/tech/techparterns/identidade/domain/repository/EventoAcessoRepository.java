package tech.techparterns.identidade.domain.repository;

import tech.techparterns.identidade.domain.model.EventoAcesso;

public interface EventoAcessoRepository {

    void salvar(EventoAcesso evento);
}
