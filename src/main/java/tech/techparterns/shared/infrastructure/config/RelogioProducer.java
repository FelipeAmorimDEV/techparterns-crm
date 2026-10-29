package tech.techparterns.shared.infrastructure.config;

import java.time.Clock;
import java.time.ZoneId;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
public class RelogioProducer {

    public static final ZoneId ZONA_PADRAO = ZoneId.of("America/Sao_Paulo");

    @Produces
    @ApplicationScoped
    Clock clock() {
        return Clock.system(ZONA_PADRAO);
    }
}
