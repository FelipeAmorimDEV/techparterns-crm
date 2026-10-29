package tech.techparterns.funil.domain.model;

import java.util.Objects;

import tech.techparterns.shared.domain.Result;

public final class TextoPlano {

    public static final String VAZIO = "Plano é obrigatório";
    public static final String LONGO = "Plano deve ter no máximo 40 caracteres";

    private final String valor;

    private TextoPlano(String valor) {
        this.valor = valor;
    }

    public static Result<TextoPlano> tryCreate(String valor) {
        String aparado = valor == null ? "" : valor.trim();
        if (aparado.isEmpty())
            return Result.fail(VAZIO);
        if (aparado.length() > 40)
            return Result.fail(LONGO);
        return Result.ok(new TextoPlano(aparado));
    }

    public static TextoPlano reconstituir(String valor) {
        return new TextoPlano(valor);
    }

    public String valor() {
        return valor;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof TextoPlano outro && Objects.equals(valor, outro.valor);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(valor);
    }

    @Override
    public String toString() {
        return valor;
    }
}
