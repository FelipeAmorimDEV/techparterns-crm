package tech.techparterns.funil.domain.model;

import tech.techparterns.shared.domain.Result;

public final class OrdemEtapa {

    public static final String INVALIDA = "Ordem deve ser um inteiro positivo";

    private final int valor;

    private OrdemEtapa(int valor) {
        this.valor = valor;
    }

    public static Result<OrdemEtapa> tryCreate(Integer ordem) {
        if (ordem == null || ordem < 1)
            return Result.fail(INVALIDA);
        return Result.ok(new OrdemEtapa(ordem));
    }

    public static OrdemEtapa reconstituir(int ordem) {
        return new OrdemEtapa(ordem);
    }

    public int valor() {
        return valor;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof OrdemEtapa outra && valor == outra.valor;
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
