package tech.techparterns.distribuicao.application;

import java.time.Clock;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.transaction.Transactional;

import tech.techparterns.distribuicao.domain.model.ConfiguracaoEmpresa;
import tech.techparterns.distribuicao.domain.model.PrazoParado;
import tech.techparterns.distribuicao.domain.model.RegraAtribuicao;
import tech.techparterns.distribuicao.domain.port.FatosLead;
import tech.techparterns.distribuicao.domain.port.RegistroEnvioParado;
import tech.techparterns.distribuicao.domain.repository.DistribuicaoRepository;
import tech.techparterns.distribuicao.infrastructure.mail.AvisoParadoListener;
import tech.techparterns.shared.domain.Result;

@ApplicationScoped
public class VerificarParados {

    private final ContextoDistribuicao contexto;
    private final DistribuicaoRepository repositorio;
    private final FatosLead fatos;
    private final TrocarDono trocar;
    private final RegistroEnvioParado envios;
    private final Event<AvisoParadoListener.AvisoLeadParado> avisos;
    private final Clock clock;

    public VerificarParados(ContextoDistribuicao contexto, DistribuicaoRepository repositorio, FatosLead fatos,
                            TrocarDono trocar, RegistroEnvioParado envios,
                            Event<AvisoParadoListener.AvisoLeadParado> avisos, Clock clock) {
        this.contexto = contexto;
        this.repositorio = repositorio;
        this.fatos = fatos;
        this.trocar = trocar;
        this.envios = envios;
        this.avisos = avisos;
        this.clock = clock;
    }

    @Transactional
    public Result<Contratos.VerificacaoParado> executar() {
        var empresa = contexto.empresa();
        var config = repositorio.buscarConfiguracao(empresa).orElse(ConfiguracaoEmpresa.padrao(empresa));
        var agora = clock.instant();
        var limite = PrazoParado.limite(agora, config.diasParaParado());
        int marcados = 0;
        int devolvidos = 0;
        for (var dono : repositorio.candidatosParado(empresa, limite)) {
            if (!config.devolverParadoAFila()) {
                if (!dono.parado()) {
                    repositorio.marcarParado(empresa, dono.leadId(), agora);
                    marcados++;
                }
                continue;
            }
            var fato = fatos.buscar(empresa, dono.leadId()).orElse(null);
            if (fato == null)
                continue;
            var anterior = dono.ownerId();
            trocar.trocar(empresa, dono.leadId(), null, RegraAtribuicao.PARADO, null, null);
            var cadastro = contexto.vendedorDaEmpresa(anterior);
            if (cadastro != null) {
                var envioId = envios.registrarPendente(anterior);
                avisos.fire(new AvisoParadoListener.AvisoLeadParado(envioId, cadastro.email(), fato.nome()));
            }
            devolvidos++;
        }
        return Result.ok(new Contratos.VerificacaoParado(marcados, devolvidos));
    }
}
