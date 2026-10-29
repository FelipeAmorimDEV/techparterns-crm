package tech.techparterns.funil.domain.model;

import java.util.Locale;

import tech.techparterns.shared.domain.Result;

public final class CorEtapa {

    public static final String INVALIDA = "Cor deve ser # seguido de seis dígitos hexadecimais";

    private final String valor;

    private CorEtapa(String valor) {
        this.valor = valor;
    }

    public static Result<CorEtapa> tryCreate(String valor) {
        if (valor == null || valor.isEmpty() || !valor.matches("#[0-9A-Fa-f]{6}"))
            return Result.fail(INVALIDA);
        return Result.ok(new CorEtapa(valor));
    }

    public static CorEtapa reconstituir(String valor) {
        return new CorEtapa(valor);
    }

    public String valor() {
        return valor;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof CorEtapa outra))
            return false;
        if (valor == null)
            return outra.valor == null;
        return valor.equalsIgnoreCase(outra.valor);
    }

    @Override
    public int hashCode() {
        return valor == null ? 0 : valor.toLowerCase(Locale.ROOT).hashCode();
    }

    @Override
    public String toString() {
        return valor;
    }
}
