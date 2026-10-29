package tech.techparterns.funil.application;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import tech.techparterns.funil.domain.model.LeadGanho;

@ApplicationScoped
public class FalhaDuranteGanho {

    private volatile boolean ligada;

    void aoGanhar(@Observes LeadGanho evento) {
        if (ligada && evento != null)
            throw new RuntimeException("falha depois da escrita do ganho");
    }

    public void ligar() {
        ligada = true;
    }

    public void desligar() {
        ligada = false;
    }
}
