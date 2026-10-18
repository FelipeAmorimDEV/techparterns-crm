package tech.techparterns.distribuicao.application;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import tech.techparterns.distribuicao.domain.model.ConfigVendedor;
import tech.techparterns.distribuicao.domain.model.ConfiguracaoEmpresa;
import tech.techparterns.distribuicao.domain.model.RegraAtribuicao;
import tech.techparterns.distribuicao.domain.model.Territorio;
import tech.techparterns.distribuicao.domain.model.ValidacaoDistribuicao;
import tech.techparterns.distribuicao.domain.repository.DistribuicaoRepository;
import tech.techparterns.shared.domain.Result;

@ApplicationScoped
public class ConfiguracaoDistribuicao {

    private final ContextoDistribuicao contexto;
    private final DistribuicaoRepository repositorio;

    public ConfiguracaoDistribuicao(ContextoDistribuicao contexto, DistribuicaoRepository repositorio) {
        this.contexto = contexto;
        this.repositorio = repositorio;
    }

    @Transactional
    public Result<Void> salvarEmpresa(Contratos.Configuracao cmd) {
        if (cmd == null)
            return Result.fail(ValidacaoDistribuicao.PRAZO_INVALIDO);
        var dias = ValidacaoDistribuicao.diasParaParado(cmd.diasParaParado());
        if (dias.isFailure())
            return Result.propagate(dias);
        RegraAtribuicao regra = null;
        if (cmd.regraPadrao() != null && !cmd.regraPadrao().isBlank()) {
            var lida = regraAutomatica(cmd.regraPadrao());
            if (lida.isFailure())
                return Result.propagate(lida);
            regra = lida.getValue();
        }
        if (cmd.automatica() && regra == null)
            return Result.fail(ValidacaoDistribuicao.REGRA_OBRIGATORIA);
        var empresa = contexto.empresa();
        var ponteiro = repositorio.buscarConfiguracao(empresa).map(ConfiguracaoEmpresa::ponteiro).orElse(null);
        repositorio.salvarConfiguracao(new ConfiguracaoEmpresa(empresa, cmd.automatica(), regra, dias.getValue(),
                cmd.devolverParadoAFila(), ponteiro));
        return Result.ok();
    }

    @Transactional
    public Result<Void> salvarVendedor(UUID vendedorId, Contratos.ConfigVendedorBody cmd) {
        if (cmd == null)
            return Result.fail(ValidacaoDistribuicao.PESO_INVALIDO);
        var peso = ValidacaoDistribuicao.peso(cmd.peso());
        if (peso.isFailure())
            return Result.propagate(peso);
        var limite = ValidacaoDistribuicao.limite(cmd.maxLeadsAbertos());
        if (limite.isFailure())
            return Result.propagate(limite);
        var cadastro = contexto.vendedorDaEmpresa(vendedorId);
        if (cadastro == null || !"SELLER".equals(cadastro.papel()))
            return Result.fail(ValidacaoDistribuicao.VENDEDOR_OUTRA_EMPRESA);
        repositorio.salvarConfigVendedor(new ConfigVendedor(contexto.empresa(), vendedorId, peso.getValue(),
                limite.getValue(), cmd.participa()));
        return Result.ok();
    }

    @Transactional
    public Result<UUID> criarTerritorio(Contratos.TerritorioBody cmd) {
        if (cmd == null)
            return Result.fail(ValidacaoDistribuicao.UF_INVALIDA);
        var uf = ValidacaoDistribuicao.uf(cmd.uf());
        if (uf.isFailure() || uf.getValue() == null)
            return uf.isFailure() ? Result.propagate(uf) : Result.fail(ValidacaoDistribuicao.UF_INVALIDA);
        var cidade = ValidacaoDistribuicao.cidade(cmd.cidade());
        if (cidade.isFailure())
            return Result.propagate(cidade);
        var cadastro = contexto.vendedorDaEmpresa(cmd.vendedorId());
        if (cadastro == null || !"SELLER".equals(cadastro.papel()))
            return Result.fail(ValidacaoDistribuicao.VENDEDOR_OUTRA_EMPRESA);
        var id = UUID.randomUUID();
        repositorio.inserirTerritorio(new Territorio(id, contexto.empresa(), uf.getValue(), cidade.getValue(), cmd.vendedorId()));
        return Result.ok(id);
    }

    private static Result<RegraAtribuicao> regraAutomatica(String nome) {
        try {
            var regra = RegraAtribuicao.valueOf(nome.trim());
            if (!regra.regraAutomatica())
                return Result.fail(ValidacaoDistribuicao.REGRA_OBRIGATORIA);
            return Result.ok(regra);
        } catch (IllegalArgumentException e) {
            return Result.fail(ValidacaoDistribuicao.REGRA_OBRIGATORIA);
        }
    }
}
