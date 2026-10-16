package tech.techparterns.identidade.application.usecase;

import java.time.Clock;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import tech.techparterns.identidade.application.command.CriarConviteCommand;
import tech.techparterns.identidade.application.dto.ConviteCriadoDto;
import tech.techparterns.identidade.application.service.AuditoriaAcesso;
import tech.techparterns.identidade.domain.model.*;
import tech.techparterns.identidade.domain.port.ContextoRequisicao;
import tech.techparterns.identidade.domain.port.GeradorTokenOpaco;
import tech.techparterns.identidade.domain.repository.ConviteRepository;
import tech.techparterns.identidade.infrastructure.config.IdentidadeConfig;
import tech.techparterns.identidade.infrastructure.security.ContextoAutenticado;
import tech.techparterns.shared.domain.Result;
import tech.techparterns.shared.domain.repository.UseCase;

/** Cria o convite. Não verifica se o e-mail existe e NÃO envia e-mail; o link só aparece nesta resposta. */
@ApplicationScoped
public class CriarConviteUseCase implements UseCase<CriarConviteCommand, ConviteCriadoDto> {

    private final ConviteRepository convites;
    private final GeradorTokenOpaco gerador;
    private final AuditoriaAcesso auditoria;
    private final ContextoAutenticado contexto;
    private final ContextoRequisicao requisicao;
    private final IdentidadeConfig config;
    private final Clock clock;

    public CriarConviteUseCase(ConviteRepository convites, GeradorTokenOpaco gerador, AuditoriaAcesso auditoria,
                               ContextoAutenticado contexto, ContextoRequisicao requisicao,
                               IdentidadeConfig config, Clock clock) {
        this.convites = convites;
        this.gerador = gerador;
        this.auditoria = auditoria;
        this.contexto = contexto;
        this.requisicao = requisicao;
        this.config = config;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Result<ConviteCriadoDto> execute(CriarConviteCommand cmd) {
        EmailUsuario email = null;
        if (cmd.email() != null && !cmd.email().isBlank()) {
            var r = EmailUsuario.tryCreate(cmd.email());
            if (r.isFailure()) return Result.propagate(r);
            email = r.getValue();
        }
        String plano = gerador.gerar();
        var convite = Convite.tryCreate(contexto.empresaId(), email, gerador.hash(plano), cmd.expiraEm(),
                cmd.maxUsos(), contexto.userId(), clock.instant());
        if (convite.isFailure())
            return Result.propagate(convite);
        var c = convite.getValue();
        convites.salvar(c);
        auditoria.registrar("CONVITE_CRIADO", AuditoriaAcesso.SUCESSO, TitularTipo.APP, contexto.userId(),
                c.empresaId(), requisicao.ip(), null);
        String link = config.frontendUrl().replaceAll("/+$", "") + config.rotas().convite() + "?token=" + plano;
        return Result.ok(new ConviteCriadoDto(c.getId(), email == null ? null : email.valor(), c.expiraEm(),
                c.maxUsos(), link));
    }
}
