package tech.techparterns.lead.application.csv;

/** Linha de dados do CSV. {@code numero} é a linha no arquivo (cabeçalho = 1). Células ausentes vêm como "". */
public record LinhaCsv(int numero, String nomeContato, String telefone, String whatsapp, String email) {
}
