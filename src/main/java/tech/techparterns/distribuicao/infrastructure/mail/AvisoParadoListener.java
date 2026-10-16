package tech.techparterns.distribuicao.infrastructure.mail;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;

import tech.techparterns.distribuicao.domain.port.RegistroEnvioParado;
import tech.techparterns.identidade.domain.port.EnviadorEmail;

@ApplicationScoped
public class AvisoParadoListener {

    private final EnviadorEmail enviador;
    private final RegistroEnvioParado registros;

    public AvisoParadoListener(EnviadorEmail enviador, RegistroEnvioParado registros) {
        this.enviador = enviador;
        this.registros = registros;
    }

    void aoConfirmar(@Observes(during = TransactionPhase.AFTER_SUCCESS) AvisoLeadParado aviso) {
        try {
            enviador.enviar(aviso.destinatario(), TextoAvisoParado.ASSUNTO, TextoAvisoParado.corpo(aviso.nomeContato()));
            registros.marcar(aviso.envioId(), true);
        } catch (Exception e) {
            registros.marcar(aviso.envioId(), false);
        }
    }

    public record AvisoLeadParado(UUID envioId, String destinatario, String nomeContato) {}
}
