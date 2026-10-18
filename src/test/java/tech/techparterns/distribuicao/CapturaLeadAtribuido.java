package tech.techparterns.distribuicao;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;

import tech.techparterns.distribuicao.domain.model.LeadAtribuido;

@ApplicationScoped
public class CapturaLeadAtribuido {

    private final List<LeadAtribuido> eventos = new CopyOnWriteArrayList<>();

    void receber(@Observes(during = TransactionPhase.AFTER_SUCCESS) LeadAtribuido evento) {
        eventos.add(evento);
    }

    public List<LeadAtribuido> eventos() {
        return List.copyOf(eventos);
    }

    public void limpar() {
        eventos.clear();
    }
}
