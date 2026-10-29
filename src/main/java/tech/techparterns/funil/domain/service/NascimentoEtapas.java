package tech.techparterns.funil.domain.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import tech.techparterns.funil.domain.model.CorEtapa;
import tech.techparterns.funil.domain.model.Etapa;
import tech.techparterns.funil.domain.model.OrdemEtapa;
import tech.techparterns.funil.domain.model.Probabilidade;
import tech.techparterns.funil.domain.model.SlaDias;
import tech.techparterns.funil.domain.model.TipoEtapa;
import tech.techparterns.shared.domain.CompanyId;
import tech.techparterns.shared.domain.exception.ErroDominio;

public final class NascimentoEtapas {

    public static final String ORDEM_REPETIDA = "Já existe etapa com essa ordem na empresa";
    public static final String GANHO_ATIVA_EXISTE = "Já existe etapa GANHO ativa";
    public static final String PERDIDO_ATIVA_EXISTE = "Já existe etapa PERDIDO ativa";

    private static final List<TipoEtapa> TIPOS = List.of(TipoEtapa.ABERTO, TipoEtapa.GANHO, TipoEtapa.PERDIDO);

    private NascimentoEtapas() {
    }

    public record Resultado(List<Etapa> etapas, List<String> erros) {
        public Resultado {
            etapas = List.copyOf(etapas);
            erros = List.copyOf(erros);
        }

        public boolean ok() {
            return erros.isEmpty();
        }
    }

    public static final class Pedido {
        private final String nome;
        private final TipoEtapa tipo;
        private final OrdemEtapa ordem;
        private final boolean ativa;
        private final Probabilidade probabilidade;
        private final CorEtapa cor;
        private final SlaDias sla;
        private final boolean exigeQualificacao;
        private final boolean exigeDemo;

        public Pedido(String nome, TipoEtapa tipo, OrdemEtapa ordem, boolean ativa,
                      Probabilidade probabilidade, CorEtapa cor, SlaDias sla,
                      boolean exigeQualificacao, boolean exigeDemo) {
            this.nome = nome;
            this.tipo = tipo;
            this.ordem = ordem;
            this.ativa = ativa;
            this.probabilidade = probabilidade;
            this.cor = cor;
            this.sla = sla;
            this.exigeQualificacao = exigeQualificacao;
            this.exigeDemo = exigeDemo;
        }

        public String nome() {
            return nome;
        }

        public TipoEtapa tipo() {
            return tipo;
        }

        public OrdemEtapa ordem() {
            return ordem;
        }

        public boolean ativa() {
            return ativa;
        }

        public Probabilidade probabilidade() {
            return probabilidade;
        }

        public CorEtapa cor() {
            return cor;
        }

        public SlaDias sla() {
            return sla;
        }

        public boolean exigeQualificacao() {
            return exigeQualificacao;
        }

        public boolean exigeDemo() {
            return exigeDemo;
        }
    }

    public static Resultado descrever(CompanyId empresa, List<Etapa> existentes, Pedido pedido) {
        if (empresa == null || empresa.valor() == null || empresa.valor().isBlank())
            return new Resultado(List.of(), List.of(ErroDominio.EMPRESA_OBRIGATORIA.mensagem()));

        List<Etapa> daEmpresa = daEmpresa(empresa, existentes);
        if (pedido != null) {
            List<String> erros = errosDoPedido(daEmpresa, pedido);
            if (!erros.isEmpty())
                return new Resultado(List.of(), erros);
        }

        Set<Integer> ocupadas = ordensOcupadas(daEmpresa);
        if (pedido != null && pedido.ordem() != null)
            ocupadas.add(pedido.ordem().valor());

        List<Etapa> etapas = new ArrayList<>();
        if (pedido != null)
            etapas.add(Etapa.definida(
                    empresa,
                    pedido.nome(),
                    pedido.tipo(),
                    pedido.ordem(),
                    pedido.ativa(),
                    pedido.probabilidade(),
                    pedido.cor(),
                    pedido.sla() == null ? SlaDias.reconstituir(null) : pedido.sla(),
                    pedido.exigeQualificacao(),
                    pedido.exigeDemo()));

        for (TipoEtapa tipo : TIPOS) {
            if (pedido != null && pedido.tipo() == tipo)
                continue;
            if (temAtiva(daEmpresa, tipo))
                continue;
            int ordem = menorOrdemLivre(ocupadas);
            ocupadas.add(ordem);
            etapas.add(Etapa.nascida(empresa, tipo, OrdemEtapa.reconstituir(ordem)));
        }
        return new Resultado(etapas, List.of());
    }

    private static List<String> errosDoPedido(List<Etapa> daEmpresa, Pedido pedido) {
        List<String> erros = new ArrayList<>();
        if (pedido.ordem() != null && ordemRepetida(daEmpresa, pedido.ordem().valor()))
            erros.add(ORDEM_REPETIDA);
        if (pedido.tipo() == TipoEtapa.GANHO && temAtiva(daEmpresa, TipoEtapa.GANHO))
            erros.add(GANHO_ATIVA_EXISTE);
        if (pedido.tipo() == TipoEtapa.PERDIDO && temAtiva(daEmpresa, TipoEtapa.PERDIDO))
            erros.add(PERDIDO_ATIVA_EXISTE);
        return erros;
    }

    private static boolean ordemRepetida(List<Etapa> daEmpresa, int ordem) {
        for (Etapa etapa : daEmpresa) {
            if (etapa.ordem() != null && etapa.ordem().valor() == ordem)
                return true;
        }
        return false;
    }

    private static boolean temAtiva(List<Etapa> daEmpresa, TipoEtapa tipo) {
        for (Etapa etapa : daEmpresa) {
            if (etapa.tipo() == tipo && etapa.ativa())
                return true;
        }
        return false;
    }

    private static Set<Integer> ordensOcupadas(List<Etapa> daEmpresa) {
        Set<Integer> ocupadas = new HashSet<>();
        for (Etapa etapa : daEmpresa) {
            if (etapa.ordem() != null)
                ocupadas.add(etapa.ordem().valor());
        }
        return ocupadas;
    }

    private static int menorOrdemLivre(Set<Integer> ocupadas) {
        int ordem = 1;
        while (ocupadas.contains(ordem))
            ordem++;
        return ordem;
    }

    private static List<Etapa> daEmpresa(CompanyId empresa, List<Etapa> existentes) {
        if (existentes == null || existentes.isEmpty())
            return List.of();
        List<Etapa> daEmpresa = new ArrayList<>();
        for (Etapa etapa : existentes) {
            if (etapa != null && etapa.companyId() != null && empresa.valor().equals(etapa.companyId().valor()))
                daEmpresa.add(etapa);
        }
        return daEmpresa;
    }
}
