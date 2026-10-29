package tech.techparterns.funil.application;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;

import tech.techparterns.funil.domain.model.LeadGanho;
import tech.techparterns.funil.domain.model.LeadMudouEtapa;
import tech.techparterns.funil.domain.model.LeadPerdido;

@ApplicationScoped
public class CapturaEventosFunil {

    private final List<Object> eventos = new CopyOnWriteArrayList<>();

    void mudou(@Observes(during = TransactionPhase.AFTER_SUCCESS) LeadMudouEtapa evento) {
        eventos.add(evento);
    }

    void ganho(@Observes(during = TransactionPhase.AFTER_SUCCESS) LeadGanho evento) {
        eventos.add(evento);
    }

    void perdido(@Observes(during = TransactionPhase.AFTER_SUCCESS) LeadPerdido evento) {
        eventos.add(evento);
    }

    public List<Object> eventos() {
        return List.copyOf(eventos);
    }

    public long ocorrencias(Class<?> tipo) {
        return eventos.stream().filter(evento -> evento.getClass() == tipo).count();
    }

    public void limpar() {
        eventos.clear();
    }
}
