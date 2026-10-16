package tech.techparterns.distribuicao.application;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import tech.techparterns.distribuicao.domain.model.ValidacaoDistribuicao;
import tech.techparterns.distribuicao.domain.port.FatosLead;
import tech.techparterns.distribuicao.domain.repository.DistribuicaoRepository;
import tech.techparterns.lead.domain.model.SituacaoLead;
import tech.techparterns.shared.domain.Result;
import tech.techparterns.shared.domain.exception.RecursoNaoEncontradoException;

@ApplicationScoped
public class EditarFatosLead {

    private final ContextoDistribuicao contexto;
    private final FatosLead fatos;
    private final DistribuicaoRepository repositorio;

    public EditarFatosLead(ContextoDistribuicao contexto, FatosLead fatos, DistribuicaoRepository repositorio) {
        this.contexto = contexto;
        this.fatos = fatos;
        this.repositorio = repositorio;
    }

    @Transactional
    public Result<Void> editar(UUID leadId, Contratos.Fatos cmd) {
        if (cmd == null || cmd.situacao() == null)
            return Result.fail("Situação inválida");
        SituacaoLead situacao;
        try {
            situacao = SituacaoLead.valueOf(cmd.situacao().trim());
        } catch (IllegalArgumentException e) {
            return Result.fail("Situação inválida");
        }
        var empresa = contexto.empresa();
        var fato = fatos.buscar(empresa, leadId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Lead não encontrado"));
        if (!"ABERTA".equals(fato.situacao()))
            return Result.fail("Situação final não muda");
        String uf = fato.uf();
        String cidade = fato.cidade();
        if (contexto.admin()) {
            var ufNova = ValidacaoDistribuicao.uf(cmd.uf());
            if (ufNova.isFailure())
                return Result.propagate(ufNova);
            var cidadeNova = ValidacaoDistribuicao.cidade(cmd.cidade());
            if (cidadeNova.isFailure())
                return Result.propagate(cidadeNova);
            uf = ufNova.getValue();
            cidade = cidadeNova.getValue();
        } else {
            var dono = repositorio.buscarDono(empresa, leadId).orElse(null);
            if (dono == null || dono.ownerId() == null || !dono.ownerId().equals(contexto.usuario()))
                return Result.fail(ValidacaoDistribuicao.NAO_E_DONO);
        }
        fatos.atualizar(empresa, leadId, situacao.name(), cmd.quente(), uf, cidade);
        return Result.ok();
    }
}
