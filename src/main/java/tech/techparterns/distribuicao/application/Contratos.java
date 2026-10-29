package tech.techparterns.distribuicao.application;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public final class Contratos {

    private Contratos() {}

    public record AtribuirManual(UUID vendedorId, List<UUID> leadIds) {}

    public record Lote(String regra, String uf, String cidade, Boolean quente, String origem) {}

    public record Carteira(String regra) {}

    public record Devolucao(UUID leadId, String motivo) {}

    public record Fatos(String situacao, boolean quente, String uf, String cidade,
                        String planoFechado, BigDecimal valorFechado, UUID motivoPerdaId, String observacao) {}

    public record Configuracao(boolean automatica, String regraPadrao, int diasParaParado, boolean devolverParadoAFila) {}

    public record ConfigVendedorBody(int peso, Integer maxLeadsAbertos, boolean participa) {}

    public record TerritorioBody(String uf, String cidade, UUID vendedorId) {}

    public record Contagem(UUID vendedorId, int quantidade) {}

    public record ResultadoLote(List<Contagem> porVendedor, int semDono) {}

    public record AtribuicaoManual(int atribuidos, int ignorados) {}

    public record ProximoFila(UUID leadId) {}

    public record LeadSemDono(UUID id, String nome, String situacao, String uf, String cidade, boolean quente, String origem) {}

    public record LinhaHistorico(UUID id, UUID leadId, UUID donoAnterior, UUID donoNovo, String regra, String motivo,
                                 UUID autorId, java.time.Instant ocorridoEm) {}

    public record VerificacaoParado(int marcados, int devolvidos) {}
}
