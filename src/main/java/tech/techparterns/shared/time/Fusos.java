package tech.techparterns.shared.time;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;

public final class Fusos {

    public static final ZoneId BRASIL = ZoneId.of("America/Sao_Paulo");

    private Fusos() {
    }

    public static LocalDate hoje() {
        return LocalDate.now(BRASIL);
    }

    public static LocalDate dataLocal(OffsetDateTime instante) {
        if (instante == null)
            return null;
        return instante.atZoneSameInstant(BRASIL).toLocalDate();
    }

    public static OffsetDateTime inicioDoDia(LocalDate data) {
        return data.atStartOfDay(BRASIL).toOffsetDateTime();
    }

    public static OffsetDateTime fimDoDia(LocalDate data) {
        return data.atTime(23, 59, 59).atZone(BRASIL).toOffsetDateTime();
    }
}
