package tech.techparterns.funil.domain.model;

import tech.techparterns.shared.domain.Result;

public final class Probabilidade {

    public static final String OBRIGATORIA = "Probabilidade é obrigatória";
    public static final String FORA_DA_FAIXA = "Probabilidade deve estar entre 0 e 100";

    private final int valor;

    private Probabilidade(int valor) {
        this.valor = valor;
    }

    public static Result<Probabilidade> tryCreate(Integer valor) {
        if (valor == null)
            return Result.fail(OBRIGATORIA);
        if (valor < 0 || valor > 100)
            return Result.fail(FORA_DA_FAIXA);
        return Result.ok(new Probabilidade(valor));
    }

    public static Probabilidade reconstituir(int valor) {
        return new Probabilidade(valor);
    }

    public int valor() {
        return valor;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Probabilidade outra && valor == outra.valor;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(valor);
    }

    @Override
    public String toString() {
        return Integer.toString(valor);
    }
}
