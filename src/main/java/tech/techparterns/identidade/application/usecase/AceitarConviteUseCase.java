package tech.techparterns.identidade.application.usecase;

import java.time.Clock;

import jakarta.enterprise.context.ApplicationScoped;

import tech.techparterns.identidade.application.command.AceitarConviteCommand;
import tech.techparterns.identidade.application.dto.RespostaDto;
import tech.techparterns.identidade.application.service.AuditoriaAcesso;
import tech.techparterns.identidade.domain.model.*;
import tech.techparterns.identidade.domain.port.*;
import tech.techparterns.identidade.domain.port.Transacao.ViolacaoUnicidadeException;
import tech.techparterns.identidade.domain.repository.*;
import tech.techparterns.shared.domain.Result;
import tech.techparterns.shared.domain.exception.ErroHttpException;
import tech.techparterns.shared.domain.repository.UseCase;

/**
 * Aceite de convite: cria SELLER ACTIVE e incrementa {@code usos} de forma atômica, numa única transação, sem abrir
 * sessão. Qualquer falha do convite (inclusive token inexistente e e-mail em uso) devolve o mesmo 400
 * {@code CONVITE_INDISPONIVEL}, sem consumir uso (RN-IA-19c).
 */
@ApplicationScoped
public class AceitarConviteUseCase implements UseCase<AceitarConviteCommand, RespostaDto> {

    private final ConviteRepository convites;
    private final EmpresaRepository empresas;
    private final UsuarioEmpresaRepository usuarios;
    private final EmailRegistradoRepository emails;
    private final GeradorTokenOpaco gerador;
    private final HasherSenha hasher;
    private final AuditoriaAcesso auditoria;
    private final Transacao transacao;
    private final ContextoRequisicao requisicao;
    private final Clock clock;

    public AceitarConviteUseCase(ConviteRepository convites, EmpresaRepository empresas,
                                 UsuarioEmpresaRepository usuarios, EmailRegistradoRepository emails,
                                 GeradorTokenOpaco gerador, HasherSenha hasher, AuditoriaAcesso auditoria,
                                 Transacao transacao, ContextoRequisicao requisicao, Clock clock) {
        this.convites = convites;
        this.empresas = empresas;
        this.usuarios = usuarios;
        this.emails = emails;
        this.gerador = gerador;
        this.hasher = hasher;
        this.auditoria = auditoria;
        this.transacao = transacao;
        this.requisicao = requisicao;
        this.clock = clock;
    }

    @Override
    public Result<RespostaDto> execute(AceitarConviteCommand cmd) {
        var nome = NomeUsuario.tryCreate(cmd == null ? null : cmd.nome());
        var email = EmailUsuario.tryCreate(cmd == null ? null : cmd.email());
        var telefone = TelefoneUsuario.tryCreateOpcional(cmd == null ? null : cmd.telefone());
        var senha = SenhaPlana.tryCreate(cmd == null ? null : cmd.senha());
        var validacao = Result.combineAll(nome, email, telefone, senha);
        if (validacao.isFailure())
            return Result.propagate(validacao);

        try {
            transacao.emNovaTransacao(() -> {
                aceitar(cmd == null ? null : cmd.token(), nome.getValue(), email.getValue(), telefone.getValue(),
                        senha.getValue());
                return Boolean.TRUE;
            });
        } catch (ViolacaoUnicidadeException e) {
            throw indisponivel();
        }
        return Result.ok(new RespostaDto("Cadastro concluído. Faça login para continuar."));
    }

    private void aceitar(String token, NomeUsuario nome, EmailUsuario email, TelefoneUsuario telefone, SenhaPlana senha) {
        if (token == null || token.isBlank())
            throw indisponivel();
        var agora = clock.instant();
        var convite = convites.buscarPorHash(gerador.hash(token)).orElseThrow(AceitarConviteUseCase::indisponivel);
        var empresa = empresas.buscarPorId(convite.empresaId()).orElseThrow(AceitarConviteUseCase::indisponivel);
        if (!convite.estaUtilizavel(agora) || !empresa.estaAtiva() || !convite.aceitaEmail(email) || emails.emUso(email))
            throw indisponivel();

        var usuario = UsuarioEmpresa.criarAtivoPorConvite(convite.empresaId(), nome, email, telefone,
                hasher.hash(senha)).getOrThrow();
        usuarios.salvar(usuario);
        if (!convites.incrementarUsoSePossivel(convite.getId(), agora))
            throw indisponivel();
        auditoria.registrar("CONVITE_ACEITO", AuditoriaAcesso.SUCESSO, TitularTipo.APP, usuario.getId(),
                convite.empresaId(), requisicao.ip(), null);
    }

    private static ErroHttpException indisponivel() {
        return new ErroHttpException(400, "CONVITE_INDISPONIVEL", ErroIdentidade.CONVITE_INDISPONIVEL.mensagem());
    }
}
