package tech.techparterns.distribuicao.domain.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import tech.techparterns.distribuicao.domain.model.RegraAtribuicao;

public final class MotorDistribuicao {

    private MotorDistribuicao() {}

    public record Vendedor(UUID id, Instant criadoEm, boolean ativo, boolean vendedor, boolean participa, int peso,
                           Integer limite, int abertos) {
        public Vendedor comAbertos(int n) {
            return new Vendedor(id, criadoEm, ativo, vendedor, participa, peso, limite, n);
        }

        public boolean elegivel() {
            if (!ativo || !vendedor || !participa)
                return false;
            return limite == null || abertos < limite;
        }
    }

    public record TerritorioRef(String uf, String cidade, UUID vendedorId) {}

    public record Escolha(UUID vendedorId, RegraAtribuicao regra, Integer ponteiro) {}

    public static boolean cidadesIguais(String a, String b) {
        if (a == null || b == null)
            return false;
        return a.trim().equals(b.trim());
    }

    public static boolean ufsIguais(String a, String b) {
        if (a == null || b == null)
            return false;
        return a.trim().equalsIgnoreCase(b.trim());
    }

    /** 0 quente, 1 não quente comum, 2 não quente devolvido ou parado. */
    public static int grupoFila(boolean quente, boolean ultimaIdaDevolucaoOuParado) {
        if (quente)
            return 0;
        return ultimaIdaDevolucaoOuParado ? 2 : 1;
    }

    public static Escolha escolher(RegraAtribuicao regra, String uf, String cidade, List<Vendedor> vendedores,
                                   List<TerritorioRef> territorios, Integer ponteiro) {
        return switch (regra) {
            case RODIZIO -> porVagas(vendedores, ponteiro, false);
            case PESO -> porVagas(vendedores, ponteiro, true);
            case CARGA -> porCarga(vendedores, ponteiro);
            case TERRITORIO -> porTerritorio(uf, cidade, vendedores, territorios, ponteiro);
            default -> new Escolha(null, regra, ponteiro);
        };
    }

    private static Escolha porVagas(List<Vendedor> vendedores, Integer ponteiro, boolean peso) {
        var slots = new ArrayList<UUID>();
        for (var v : ordenar(vendedores)) {
            if (!v.elegivel())
                continue;
            int vagas = peso ? Math.max(v.peso(), 0) : 1;
            if (peso && vagas == 0)
                continue;
            for (int n = 0; n < vagas; n++)
                slots.add(v.id());
        }
        var regra = peso ? RegraAtribuicao.PESO : RegraAtribuicao.RODIZIO;
        if (slots.isEmpty())
            return new Escolha(null, regra, ponteiro);
        int i = ponteiro == null ? 0 : Math.floorMod(ponteiro + 1, slots.size());
        return new Escolha(slots.get(i), regra, i);
    }

    private static Escolha porCarga(List<Vendedor> vendedores, Integer ponteiro) {
        var elegiveis = ordenar(vendedores).stream().filter(Vendedor::elegivel).toList();
        if (elegiveis.isEmpty())
            return new Escolha(null, RegraAtribuicao.CARGA, ponteiro);
        int min = elegiveis.stream().mapToInt(Vendedor::abertos).min().orElseThrow();
        var empatados = elegiveis.stream().filter(v -> v.abertos() == min).toList();
        if (empatados.size() == 1)
            return new Escolha(empatados.get(0).id(), RegraAtribuicao.CARGA, ponteiro);
        int i = ponteiro == null ? 0 : Math.floorMod(ponteiro + 1, empatados.size());
        return new Escolha(empatados.get(i).id(), RegraAtribuicao.CARGA, i);
    }

    private static Escolha porTerritorio(String uf, String cidade, List<Vendedor> vendedores,
                                         List<TerritorioRef> territorios, Integer ponteiro) {
        TerritorioRef cidadeMatch = null;
        if (cidade != null && !cidade.isBlank()) {
            for (var t : territorios) {
                if (t.cidade() != null && cidadesIguais(cidade, t.cidade()) && ufsIguais(uf, t.uf())) {
                    cidadeMatch = t;
                    break;
                }
            }
        }
        if (cidadeMatch != null) {
            var v = achar(vendedores, cidadeMatch.vendedorId());
            if (v != null && v.elegivel())
                return new Escolha(v.id(), RegraAtribuicao.TERRITORIO, ponteiro);
            return porVagas(vendedores, ponteiro, false);
        }
        for (var t : territorios) {
            if ((t.cidade() == null || t.cidade().isBlank()) && ufsIguais(uf, t.uf())) {
                var v = achar(vendedores, t.vendedorId());
                if (v != null && v.elegivel())
                    return new Escolha(v.id(), RegraAtribuicao.TERRITORIO, ponteiro);
                break;
            }
        }
        return porVagas(vendedores, ponteiro, false);
    }

    private static Vendedor achar(List<Vendedor> vendedores, UUID id) {
        for (var v : vendedores)
            if (v.id().equals(id))
                return v;
        return null;
    }

    private static List<Vendedor> ordenar(List<Vendedor> vendedores) {
        return vendedores.stream()
                .sorted(Comparator.comparing(Vendedor::criadoEm).thenComparing(Vendedor::id))
                .toList();
    }
}
