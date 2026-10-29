package tech.techparterns.lead.domain.model;

import java.util.Set;
import java.util.regex.Pattern;

import tech.techparterns.shared.domain.Result;

public final class TelefoneBrasil {

    public static final String TELEFONE_INVALIDO = "Telefone inválido";
    public static final String WHATSAPP_INVALIDO = "WhatsApp inválido";

    private static final Set<String> DDDS = Set.of(
            "11", "12", "13", "14", "15", "16", "17", "18", "19",
            "21", "22", "24", "27", "28",
            "31", "32", "33", "34", "35", "37", "38",
            "41", "42", "43", "44", "45", "46", "47", "48", "49",
            "51", "53", "54", "55",
            "61", "62", "63", "64", "65", "66", "67", "68", "69",
            "71", "73", "74", "75", "77", "79",
            "81", "82", "83", "84", "85", "86", "87", "88", "89",
            "91", "92", "93", "94", "95", "96", "97", "98", "99");

    private static final Pattern FORMA = Pattern.compile("^\\+55(\\d{2})(9\\d{8}|\\d{8})$");

    private final String valor;

    private TelefoneBrasil(String valor) {
        this.valor = valor;
    }

    public static Result<TelefoneBrasil> tryCreate(String valor) {
        return criar(valor, TELEFONE_INVALIDO);
    }

    public static Result<TelefoneBrasil> tryCreateWhatsapp(String valor) {
        return criar(valor, WHATSAPP_INVALIDO);
    }

    private static Result<TelefoneBrasil> criar(String valor, String mensagem) {
        if (valor == null)
            return Result.fail(mensagem);
        String s = valor.trim().replaceAll("[\\s()\\-./]", "");
        if (s.startsWith("55"))
            s = "+" + s;
        var m = FORMA.matcher(s);
        if (!m.matches() || !DDDS.contains(m.group(1)))
            return Result.fail(mensagem);
        return Result.ok(new TelefoneBrasil(s));
    }

    public static TelefoneBrasil reconstituir(String valor) {
        return new TelefoneBrasil(valor);
    }

    public String valor() { return valor; }

    @Override
    public boolean equals(Object o) {
        return o instanceof TelefoneBrasil t && valor.equals(t.valor);
    }

    @Override
    public int hashCode() { return valor.hashCode(); }

    @Override
    public String toString() { return valor; }
}
