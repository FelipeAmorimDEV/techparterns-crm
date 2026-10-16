package tech.techparterns.distribuicao.domain.model;

import tech.techparterns.shared.domain.Result;

public final class ValidacaoDistribuicao {

    public static final String MOTIVO_OBRIGATORIO = "Motivo é obrigatório";
    public static final String MOTIVO_LONGO = "Motivo acima de 200 caracteres";
    public static final String PESO_INVALIDO = "Peso fora de 0 a 10";
    public static final String PRAZO_INVALIDO = "Prazo de parado menor que 1";
    public static final String LIMITE_INVALIDO = "Limite inválido";
    public static final String UF_INVALIDA = "UF inválida";
    public static final String CIDADE_INVALIDA = "Cidade inválida";
    public static final String VENDEDOR_INVALIDO = "Vendedor destino inválido";
    public static final String VENDEDOR_OUTRA_EMPRESA = "Vendedor de outra empresa";
    public static final String REGRA_OBRIGATORIA = "Regra padrão é obrigatória para ligar a automática";
    public static final String NAO_E_DONO = "Vendedor não é o dono";
    public static final String FILA_INDISPONIVEL = "Não há lead na fila";
    public static final String FORA_DA_FILA = "Vendedor não pode pegar da fila";

    private ValidacaoDistribuicao() {}

    public static Result<String> motivoDevolucao(String valor) {
        if (valor == null || valor.isBlank())
            return Result.fail(MOTIVO_OBRIGATORIO);
        var t = valor.trim();
        if (t.length() > 200)
            return Result.fail(MOTIVO_LONGO);
        return Result.ok(t);
    }

    public static Result<Integer> peso(int peso) {
        if (peso < 0 || peso > 10)
            return Result.fail(PESO_INVALIDO);
        return Result.ok(peso);
    }

    public static Result<Integer> diasParaParado(int dias) {
        if (dias < 1)
            return Result.fail(PRAZO_INVALIDO);
        return Result.ok(dias);
    }

    public static Result<Integer> limite(Integer limite) {
        if (limite != null && limite < 0)
            return Result.fail(LIMITE_INVALIDO);
        return Result.ok(limite);
    }

    public static Result<String> uf(String valor) {
        if (valor == null || valor.isBlank())
            return Result.ok(null);
        var t = valor.trim();
        if (t.length() != 2 || !t.chars().allMatch(Character::isLetter))
            return Result.fail(UF_INVALIDA);
        return Result.ok(t.toUpperCase(java.util.Locale.ROOT));
    }

    public static Result<String> cidade(String valor) {
        if (valor == null || valor.isBlank())
            return Result.ok(null);
        var t = valor.trim();
        if (t.length() > 80)
            return Result.fail(CIDADE_INVALIDA);
        return Result.ok(t);
    }
}
