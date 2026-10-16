package tech.techparterns.lead.application.csv;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import tech.techparterns.shared.domain.Result;

public final class CsvLeads {

    public static final int MAX_LINHAS = 50_000;
    public static final String NAO_UTF8 = "Arquivo não é UTF-8 válido";
    public static final String LINHAS_EXCEDIDAS = "Arquivo acima de 50 mil linhas";
    public static final String CABECALHO_INVALIDO = "Cabeçalho obrigatório inválido";

    private static final List<String> COLUNAS = List.of("nome_contato", "telefone", "whatsapp", "email");

    private CsvLeads() {}

    public static Result<List<LinhaCsv>> ler(byte[] bytes) {
        String texto;
        try {
            texto = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException e) {
            return Result.fail(NAO_UTF8);
        }
        if (texto.startsWith("\uFEFF"))
            texto = texto.substring(1);

        List<String> fisicas = separarLinhas(texto);
        if (fisicas.size() - 1 > MAX_LINHAS)
            return Result.fail(LINHAS_EXCEDIDAS);
        if (fisicas.isEmpty())
            return Result.fail(CABECALHO_INVALIDO);

        var cabecalho = dividir(fisicas.get(0));
        int[] indice = new int[COLUNAS.size()];
        for (int c = 0; c < COLUNAS.size(); c++) {
            int achado = -1;
            for (int i = 0; i < cabecalho.size(); i++) {
                if (cabecalho.get(i).trim().equals(COLUNAS.get(c))) {
                    if (achado >= 0)
                        return Result.fail(CABECALHO_INVALIDO);
                    achado = i;
                }
            }
            if (achado < 0)
                return Result.fail(CABECALHO_INVALIDO);
            indice[c] = achado;
        }

        var linhas = new ArrayList<LinhaCsv>(fisicas.size() - 1);
        for (int n = 1; n < fisicas.size(); n++) {
            var celulas = dividir(fisicas.get(n));
            linhas.add(new LinhaCsv(n + 1,
                    celula(celulas, indice[0]), celula(celulas, indice[1]),
                    celula(celulas, indice[2]), celula(celulas, indice[3])));
        }
        return Result.ok(linhas);
    }

    private static String celula(List<String> celulas, int i) {
        return i < celulas.size() ? celulas.get(i) : "";
    }

    private static List<String> separarLinhas(String texto) {
        var linhas = new ArrayList<String>();
        int inicio = 0;
        int n = texto.length();
        for (int i = 0; i < n; i++) {
            if (texto.charAt(i) == '\n') {
                int fim = (i > inicio && texto.charAt(i - 1) == '\r') ? i - 1 : i;
                linhas.add(texto.substring(inicio, fim));
                inicio = i + 1;
            }
        }
        if (inicio < n)
            linhas.add(texto.substring(inicio));
        return linhas;
    }

    private static List<String> dividir(String linha) {
        var celulas = new ArrayList<String>();
        var atual = new StringBuilder();
        boolean entreAspas = false;
        for (int i = 0; i < linha.length(); i++) {
            char c = linha.charAt(i);
            if (entreAspas) {
                if (c == '"') {
                    if (i + 1 < linha.length() && linha.charAt(i + 1) == '"') {
                        atual.append('"');
                        i++;
                    } else {
                        entreAspas = false;
                    }
                } else {
                    atual.append(c);
                }
            } else if (c == '"' && atual.isEmpty()) {
                entreAspas = true;
            } else if (c == ',') {
                celulas.add(atual.toString());
                atual.setLength(0);
            } else {
                atual.append(c);
            }
        }
        celulas.add(atual.toString());
        return celulas;
    }
}
