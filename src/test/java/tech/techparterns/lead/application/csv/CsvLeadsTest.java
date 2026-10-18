package tech.techparterns.lead.application.csv;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class CsvLeadsTest {

    private static byte[] b(String s) { return s.getBytes(StandardCharsets.UTF_8); }

    @Test
    void bomNaoFazParteDoCabecalho() {
        var r = CsvLeads.ler(b("\uFEFFnome_contato,telefone,whatsapp,email\nAna,,,a@x.com\n"));
        assertTrue(r.isOk());
        assertEquals("Ana", r.getValue().getFirst().nomeContato());
        assertEquals(2, r.getValue().getFirst().numero());
    }

    @Test
    void utf8InvalidoFalha() {
        var r = CsvLeads.ler(new byte[] {'a', ',', (byte) 0xC3, (byte) 0x28});
        assertEquals("Arquivo não é UTF-8 válido", r.getErrors().getFirst());
    }

    @Test
    void cabecalhoSemEmailFalha() {
        var r = CsvLeads.ler(b("nome_contato,telefone,whatsapp\nAna,1,2\n"));
        assertEquals("Cabeçalho obrigatório inválido", r.getErrors().getFirst());
    }

    @Test
    void cabecalhoRepetidoFalha() {
        var r = CsvLeads.ler(b("nome_contato,telefone,telefone,whatsapp,email\n"));
        assertEquals("Cabeçalho obrigatório inválido", r.getErrors().getFirst());
    }

    @Test
    void colunaExtraIgnoradaELeituraPorNome() {
        var r = CsvLeads.ler(b("fazenda,email, nome_contato ,whatsapp,telefone\nSol,a@x.com,Ana,w,t\n"));
        assertTrue(r.isOk());
        var l = r.getValue().getFirst();
        assertEquals("Ana", l.nomeContato());
        assertEquals("t", l.telefone());
        assertEquals("w", l.whatsapp());
        assertEquals("a@x.com", l.email());
    }

    @Test
    void aspasVirgulaEAspasDuplicadas() {
        var r = CsvLeads.ler(b("nome_contato,telefone,whatsapp,email\r\n\"Silva, \"\"Ana\"\"\",,,a@x.com\r\n"));
        assertEquals(1, r.getValue().size());
        assertEquals("Silva, \"Ana\"", r.getValue().getFirst().nomeContato());
    }

    @Test
    void linhaEmBrancoNoMeioContaEFimNaoCriaLinha() {
        var r = CsvLeads.ler(b("nome_contato,telefone,whatsapp,email\nAna,,,a@x.com\n\nBia,,,b@x.com\n"));
        assertEquals(3, r.getValue().size());
        assertEquals("", r.getValue().get(1).nomeContato());
        assertEquals(4, r.getValue().get(2).numero());
    }

    @Test
    void apenasCabecalhoDaListaVazia() {
        assertTrue(CsvLeads.ler(b("nome_contato,telefone,whatsapp,email")).getValue().isEmpty());
    }

    @Test
    void cinquentaMilLinhasAceitasE50001Recusada() {
        var cab = "nome_contato,telefone,whatsapp,email\n";
        assertEquals(50_000, CsvLeads.ler(b(cab + "\n".repeat(50_000))).getValue().size());
        var r = CsvLeads.ler(b(cab + "\n".repeat(50_001)));
        assertEquals("Arquivo acima de 50 mil linhas", r.getErrors().getFirst());
    }
}
