package tech.techparterns.identidade.application.service;

import java.time.Clock;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import tech.techparterns.identidade.domain.model.PapelAcesso;
import tech.techparterns.identidade.domain.model.RefreshToken;
import tech.techparterns.identidade.domain.model.TitularTipo;
import tech.techparterns.identidade.domain.port.EmissorAccessToken;
import tech.techparterns.identidade.domain.port.GeradorTokenOpaco;
import tech.techparterns.identidade.domain.repository.RefreshTokenRepository;

/** Emite access token JWT e refresh token opaco (nova família ou rotação dentro da mesma família). */
@ApplicationScoped
public class SessaoService {

    public record SessaoEmitida(String accessToken, long expiraEmSegundos, String refreshTokenPlano) {}

    private final RefreshTokenRepository refreshTokens;
    private final GeradorTokenOpaco gerador;
    private final EmissorAccessToken emissor;
    private final Clock clock;

    public SessaoService(RefreshTokenRepository refreshTokens, GeradorTokenOpaco gerador,
                         EmissorAccessToken emissor, Clock clock) {
        this.refreshTokens = refreshTokens;
        this.gerador = gerador;
        this.emissor = emissor;
        this.clock = clock;
    }

    public SessaoEmitida abrir(TitularTipo tipo, UUID titularId, PapelAcesso papel, UUID empresaId, UUID familiaId) {
        String plano = gerador.gerar();
        var refresh = RefreshToken.criar(tipo, titularId, familiaId, gerador.hash(plano), clock.instant());
        refreshTokens.salvar(refresh);
        String access = emissor.emitir(titularId, papel, empresaId);
        return new SessaoEmitida(access, EmissorAccessToken.VALIDADE_SEGUNDOS, plano);
    }
}
