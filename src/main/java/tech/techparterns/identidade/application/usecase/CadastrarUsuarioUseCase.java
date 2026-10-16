package tech.techparterns.identidade.application.usecase;

import java.time.Clock;
import java.time.Duration;

import jakarta.enterprise.context.ApplicationScoped;

import tech.techparterns.identidade.application.command.CadastrarUsuarioCommand;
import tech.techparterns.identidade.application.dto.RespostaDto;
import tech.techparterns.identidade.application.service.AuditoriaAcesso;
import tech.techparterns.identidade.application.service.NotificadorSenha;
import tech.techparterns.identidade.domain.model.*;
import tech.techparterns.identidade.domain.port.ContextoRequisicao;
import tech.techparterns.identidade.domain.port.LimitadorTaxa;
import tech.techparterns.identidade.domain.port.Transacao;
import tech.techparterns.identidade.domain.port.Transacao.ViolacaoUnicidadeException;
import tech.techparterns.identidade.domain.repository.EmailRegistradoRepository;
import tech.techparterns.identidade.domain.repository.EmpresaRepository;
import tech.techparterns.identidade.domain.repository.UsuarioEmpresaRepository;
import tech.techparterns.identidade.infrastructure.config.IdentidadeConfig;
import tech.techparterns.identidade.infrastructure.security.ContextoAutenticado;
import tech.techparterns.shared.domain.Result;
import tech.techparterns.shared.domain.exception.LimiteExcedidoException;
import tech.techparterns.shared.domain.exception.RecursoNaoEncontradoException;
import tech.techparterns.shared.domain.repository.UseCase;

/**
 * Cadastro direto de usuário (SELLER ou ADMIN) pelo admin. Resposta neutra (202) com e-mail livre ou em uso
 * (RN-IA-19). Validação de formato vem ANTES do limite de 30/h (DEL-20).
 */
@ApplicationScoped
public class CadastrarUsuarioUseCase implements UseCase<CadastrarUsuarioCommand, RespostaDto> {

    private final UsuarioEmpresaRepository usuarios;
    private final EmpresaRepository empresas;
    private final EmailRegistradoRepository emails;
    private final NotificadorSenha notificador;
    private final AuditoriaAcesso auditoria;
    private final LimitadorTaxa limitador;
    private final Transacao transacao;
    private final ContextoAutenticado contexto;
    private final ContextoRequisicao requisicao;
    private final IdentidadeConfig config;

    public CadastrarUsuarioUseCase(UsuarioEmpresaRepository usuarios, EmpresaRepository empresas,
                                   EmailRegistradoRepository emails, NotificadorSenha notificador,
                                   AuditoriaAcesso auditoria, LimitadorTaxa limitador, Transacao transacao,
                                   ContextoAutenticado contexto, ContextoRequisicao requisicao,
                                   IdentidadeConfig config) {
        this.usuarios = usuarios;
        this.empresas = empresas;
        this.emails = emails;
        this.notificador = notificador;
        this.auditoria = auditoria;
        this.limitador = limitador;
        this.transacao = transacao;
        this.contexto = contexto;
        this.requisicao = requisicao;
        this.config = config;
    }

    @Override
    public Result<RespostaDto> execute(CadastrarUsuarioCommand cmd) {
        var empresaId = contexto.empresaId();
        var adminId = contexto.userId();

        var nome = NomeUsuario.tryCreate(cmd.nome());
        var email = EmailUsuario.tryCreate(cmd.email());
        var telefone = TelefoneUsuario.tryCreateOpcional(cmd.telefone());
        var papel = PapelParser.parse(cmd.role(), PapelUsuario.SELLER);
        var validacao = Result.combineAll(nome, email, telefone, papel);
        if (validacao.isFailure())
            return Result.propagate(validacao);

        var taxa = limitador.consumir("cadastro:" + adminId, config.rate().cadastroPorHora(), Duration.ofHours(1));
        if (!taxa.permitido())
            throw new LimiteExcedidoException("Limite de cadastros por hora excedido.", taxa.retryAfterSegundos());

        boolean criado;
        try {
            criado = transacao.emNovaTransacao(() -> criar(empresaId, adminId, nome.getValue(), email.getValue(),
                    telefone.getValue(), papel.getValue()));
        } catch (ViolacaoUnicidadeException e) {
            criado = false;
        }
        if (!criado)
            auditoria.registrarIndependente("CADASTRO_USUARIO", "EMAIL_EM_USO", TitularTipo.APP, adminId, empresaId,
                    requisicao.ip(), null);
        return Result.ok(RespostaDto.SOLICITACAO_RECEBIDA);
    }

    private boolean criar(java.util.UUID empresaId, java.util.UUID adminId, NomeUsuario nome, EmailUsuario email,
                          TelefoneUsuario telefone, PapelUsuario papel) {
        if (emails.emUso(email))
            return false;
        var empresa = empresas.buscarPorId(empresaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(ErroIdentidade.EMPRESA_NAO_ENCONTRADA.mensagem()));
        var usuario = UsuarioEmpresa.tryCreate(empresaId, nome, email, telefone, papel).getOrThrow();
        usuarios.salvar(usuario);
        notificador.notificar(usuario, empresa.nome().valor(), TipoTokenSenha.SETUP);
        auditoria.registrar("USUARIO_CRIADO", AuditoriaAcesso.SUCESSO, TitularTipo.APP, adminId, empresaId,
                requisicao.ip(), "role=" + papel.name());
        return true;
    }
}
