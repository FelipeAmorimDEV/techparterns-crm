package tech.techparterns.distribuicao.application;

import java.util.ArrayList;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import tech.techparterns.distribuicao.domain.model.RegraAtribuicao;
import tech.techparterns.distribuicao.domain.model.ValidacaoDistribuicao;
import tech.techparterns.distribuicao.domain.port.FatosLead;
import tech.techparterns.distribuicao.domain.repository.DistribuicaoRepository;
import tech.techparterns.shared.domain.Result;

@ApplicationScoped
public class OperacoesDistribuicao {

    private final ContextoDistribuicao contexto;
    private final FatosLead fatos;
    private final DistribuicaoRepository repositorio;
    private final TrocarDono trocar;
    private final ExecucaoLote lote;

    public OperacoesDistribuicao(ContextoDistribuicao contexto, FatosLead fatos, DistribuicaoRepository repositorio,
                                 TrocarDono trocar, ExecucaoLote lote) {
        this.contexto = contexto;
        this.fatos = fatos;
        this.repositorio = repositorio;
        this.trocar = trocar;
        this.lote = lote;
    }

    public java.util.List<Contratos.LeadSemDono> listarSemDono(String uf, String cidade, Boolean quente, String origem) {
        var filtro = new FatosLead.Filtro(emBranco(uf), emBranco(cidade), quente, emBranco(origem));
        return fatos.listarSemDono(contexto.empresa(), filtro).stream()
                .map(f -> new Contratos.LeadSemDono(f.id(), f.nome(), f.situacao(), f.uf(), f.cidade(), f.quente(), f.origem()))
                .toList();
    }

    @Transactional
    public Result<Contratos.AtribuicaoManual> atribuir(Contratos.AtribuirManual cmd) {
        if (cmd == null || cmd.vendedorId() == null || cmd.leadIds() == null || cmd.leadIds().isEmpty())
            return Result.fail(ValidacaoDistribuicao.VENDEDOR_INVALIDO);
        var destino = contexto.vendedorDaEmpresa(cmd.vendedorId());
        if (destino == null || !"SELLER".equals(destino.papel()) || !"ACTIVE".equals(destino.status()))
            return Result.fail(ValidacaoDistribuicao.VENDEDOR_INVALIDO);
        var empresa = contexto.empresa();
        int atribuidos = 0;
        int ignorados = 0;
        for (var leadId : cmd.leadIds()) {
            var fato = fatos.buscar(empresa, leadId).orElse(null);
            if (fato == null || fato.excluido() || !"ABERTA".equals(fato.situacao())) {
                ignorados++;
                continue;
            }
            if (trocar.trocar(empresa, leadId, cmd.vendedorId(), RegraAtribuicao.MANUAL, null, contexto.usuario()))
                atribuidos++;
            else
                ignorados++;
        }
        return Result.ok(new Contratos.AtribuicaoManual(atribuidos, ignorados));
    }

    @Transactional
    public Result<Contratos.ResultadoLote> simularLote(Contratos.Lote cmd) {
        return executarLote(cmd, false);
    }

    @Transactional
    public Result<Contratos.ResultadoLote> confirmarLote(Contratos.Lote cmd) {
        return executarLote(cmd, true);
    }

    @Transactional
    public Result<Contratos.ResultadoLote> simularCarteira(UUID vendedorId, Contratos.Carteira cmd) {
        return executarCarteira(vendedorId, cmd, false);
    }

    @Transactional
    public Result<Contratos.ResultadoLote> confirmarCarteira(UUID vendedorId, Contratos.Carteira cmd) {
        return executarCarteira(vendedorId, cmd, true);
    }

    @Transactional
    public Result<Contratos.ProximoFila> pegarFila() {
        var empresa = contexto.empresa();
        var eu = contexto.usuario();
        var cadastro = contexto.vendedorDaEmpresa(eu);
        if (cadastro == null || !"SELLER".equals(cadastro.papel()) || !"ACTIVE".equals(cadastro.status()))
            return Result.fail(ValidacaoDistribuicao.FORA_DA_FILA);
        var cfg = repositorio.buscarConfigVendedor(empresa, eu)
                .orElse(tech.techparterns.distribuicao.domain.model.ConfigVendedor.padrao(empresa, eu));
        int abertos = repositorio.cargasAbertas(empresa).getOrDefault(eu, 0);
        if (!cfg.participa() || (cfg.maxLeadsAbertos() != null && abertos >= cfg.maxLeadsAbertos()))
            return Result.fail(ValidacaoDistribuicao.FORA_DA_FILA);
        var proximo = repositorio.travarProximoDaFila(empresa);
        if (proximo.isEmpty())
            return Result.ok(new Contratos.ProximoFila(null));
        trocar.trocar(empresa, proximo.get(), eu, RegraAtribuicao.FILA, null, eu);
        return Result.ok(new Contratos.ProximoFila(proximo.get()));
    }

    @Transactional
    public Result<Void> devolver(Contratos.Devolucao cmd) {
        var motivo = ValidacaoDistribuicao.motivoDevolucao(cmd == null ? null : cmd.motivo());
        if (motivo.isFailure())
            return Result.propagate(motivo);
        if (cmd.leadId() == null)
            return Result.fail(ValidacaoDistribuicao.NAO_E_DONO);
        var empresa = contexto.empresa();
        var fato = fatos.buscar(empresa, cmd.leadId()).orElse(null);
        var dono = repositorio.buscarDono(empresa, cmd.leadId()).orElse(null);
        if (fato == null || fato.excluido() || !"ABERTA".equals(fato.situacao())
                || dono == null || !contexto.usuario().equals(dono.ownerId()))
            return Result.fail(ValidacaoDistribuicao.NAO_E_DONO);
        trocar.trocar(empresa, cmd.leadId(), null, RegraAtribuicao.DEVOLUCAO, motivo.getValue(), contexto.usuario());
        return Result.ok();
    }

    public java.util.List<Contratos.LinhaHistorico> historico(UUID leadId) {
        var empresa = contexto.empresa();
        if (fatos.buscar(empresa, leadId).isEmpty())
            throw new tech.techparterns.shared.domain.exception.RecursoNaoEncontradoException("Lead não encontrado");
        return repositorio.historico(empresa, leadId).stream()
                .map(a -> new Contratos.LinhaHistorico(a.id(), a.leadId(), a.donoAnterior(), a.donoNovo(),
                        a.regra().name(), a.motivo(), a.autorId(), a.ocorridoEm()))
                .toList();
    }

    private Result<Contratos.ResultadoLote> executarLote(Contratos.Lote cmd, boolean gravar) {
        var regra = regraLote(cmd == null ? null : cmd.regra());
        if (regra.isFailure())
            return Result.propagate(regra);
        var empresa = contexto.empresa();
        var filtro = filtro(cmd);
        var alvos = PlanejadorDistribuicao.deFatos(fatos.listarSemDono(empresa, filtro), id -> null);
        var autor = gravar ? contexto.usuario() : null;
        return Result.ok(lote.executar(empresa, regra.getValue(), alvos, gravar, autor));
    }

    private Result<Contratos.ResultadoLote> executarCarteira(UUID vendedorId, Contratos.Carteira cmd, boolean gravar) {
        var regra = regraLote(cmd == null ? null : cmd.regra());
        if (regra.isFailure())
            return Result.propagate(regra);
        var destino = contexto.vendedorDaEmpresa(vendedorId);
        if (destino == null || !"SELLER".equals(destino.papel()))
            return Result.fail(ValidacaoDistribuicao.VENDEDOR_OUTRA_EMPRESA);
        var empresa = contexto.empresa();
        var alvos = new ArrayList<PlanejadorDistribuicao.Alvo>();
        for (var fato : fatos.listarCarteiraAberta(empresa, vendedorId))
            alvos.add(new PlanejadorDistribuicao.Alvo(fato.id(), fato.criadoEm(), fato.uf(), fato.cidade(), vendedorId));
        var autor = gravar ? contexto.usuario() : null;
        return Result.ok(lote.executar(empresa, regra.getValue(), alvos, gravar, autor));
    }

    private static Result<RegraAtribuicao> regraLote(String nome) {
        if (nome == null || nome.isBlank())
            return Result.fail("Regra inválida");
        try {
            var regra = RegraAtribuicao.valueOf(nome.trim());
            if (!regra.regraDeLote())
                return Result.fail("Regra inválida");
            return Result.ok(regra);
        } catch (IllegalArgumentException e) {
            return Result.fail("Regra inválida");
        }
    }

    private static FatosLead.Filtro filtro(Contratos.Lote cmd) {
        if (cmd == null)
            return new FatosLead.Filtro(null, null, null, null);
        return new FatosLead.Filtro(emBranco(cmd.uf()), emBranco(cmd.cidade()), cmd.quente(), emBranco(cmd.origem()));
    }

    private static String emBranco(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
