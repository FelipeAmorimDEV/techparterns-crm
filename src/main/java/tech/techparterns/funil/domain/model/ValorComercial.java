package tech.techparterns.funil.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

import tech.techparterns.shared.domain.Result;

public final class ValorComercial {

    public static final String INVALIDO = "Valor comercial não pode ser nulo nem negativo";

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.UNNECESSARY);

    private final BigDecimal valor;

    private ValorComercial(BigDecimal valor) {
        this.valor = valor;
    }

    public static Result<ValorComercial> tryCreate(BigDecimal valor) {
        if (valor == null || valor.signum() < 0)
            return Result.fail(INVALIDO);
        return Result.ok(new ValorComercial(valor.setScale(2, RoundingMode.UNNECESSARY)));
    }

    public static ValorComercial de(BigDecimal valor) {
        return new ValorComercial(valor);
    }

    public static ValorComercial zero() {
        return new ValorComercial(ZERO);
    }

    public static ValorComercial reconstituir(BigDecimal valor) {
        return new ValorComercial(valor);
    }

    public BigDecimal valor() {
        return valor;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ValorComercial outro && Objects.equals(valor, outro.valor);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(valor);
    }

    @Override
    public String toString() {
        return valor == null ? "" : valor.toPlainString();
    }
}
