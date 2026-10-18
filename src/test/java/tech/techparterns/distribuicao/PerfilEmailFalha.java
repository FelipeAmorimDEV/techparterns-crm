package tech.techparterns.distribuicao;

import java.util.Set;

import io.quarkus.test.junit.QuarkusTestProfile;

public class PerfilEmailFalha implements QuarkusTestProfile {

    @Override
    public Set<Class<?>> getEnabledAlternatives() {
        return Set.of(EnviadorEmailQueFalha.class);
    }
}
