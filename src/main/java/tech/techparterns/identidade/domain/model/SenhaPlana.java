package tech.techparterns.identidade.domain.model;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import tech.techparterns.shared.domain.Result;

/** Senha em texto. Nunca persistida nem logada: {@code toString} é neutro. */
public final class SenhaPlana {

    public static final int MAX_BYTES = 72;

    private final String valor;

    private SenhaPlana(String valor) { this.valor = valor; }

    public static Result<SenhaPlana> tryCreate(String valor) {
        if (valor == null || valor.isEmpty())
            return Result.fail(ErroIdentidade.SENHA_OBRIGATORIA.mensagem());
        var erros = new ArrayList<String>();
        if (valor.length() < 8)
            erros.add(ErroIdentidade.SENHA_CURTA.mensagem());
        if (valor.codePoints().noneMatch(Character::isLetter))
            erros.add(ErroIdentidade.SENHA_SEM_LETRA.mensagem());
        if (valor.codePoints().noneMatch(Character::isDigit))
            erros.add(ErroIdentidade.SENHA_SEM_NUMERO.mensagem());
        if (excedeLimite(valor))
            erros.add(ErroIdentidade.SENHA_MUITO_LONGA.mensagem());
        return erros.isEmpty() ? Result.ok(new SenhaPlana(valor)) : Result.fail(erros);
    }

    public static boolean excedeLimite(String valor) {
        return valor != null && valor.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES;
    }

    public String valor() { return valor; }

    @Override
    public String toString() { return "SenhaPlana[****]"; }
}
