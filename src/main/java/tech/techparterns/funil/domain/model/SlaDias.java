package tech.techparterns.funil.domain.model;

import java.util.Objects;

import tech.techparterns.shared.domain.Result;

public final class SlaDias {

    public static final String INVALIDO = "SLA em dias deve ser um inteiro positivo";

    private final Integer valor;

    private SlaDias(Integer valor) {
        this.valor = valor;
    }

    public static Result<SlaDias> tryCreate(Integer dias) {
        if (dias == null)
            return Result.ok(new SlaDias(null));
        if (dias < 1)
            return Result.fail(INVALIDO);
        return Result.ok(new SlaDias(dias));
    }

    public static SlaDias reconstituir(Integer dias) {
        return new SlaDias(dias);
    }

    public boolean vazio() {
        return valor == null;
    }

    public Integer valor() {
        return valor;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof SlaDias outro && Objects.equals(valor, outro.valor);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(valor);
    }

    @Override
    public String toString() {
        return valor == null ? "" : Integer.toString(valor);
    }
}
