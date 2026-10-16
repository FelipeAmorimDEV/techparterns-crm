package tech.techparterns.identidade.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;

import tech.techparterns.identidade.application.command.CriarEmpresaCommand;
import tech.techparterns.identidade.application.dto.EmpresaCriadaDto;
import tech.techparterns.identidade.application.service.AuditoriaAcesso;
import tech.techparterns.identidade.application.service.NotificadorSenha;
import tech.techparterns.identidade.domain.model.*;
import tech.techparterns.identidade.domain.port.ContextoRequisicao;
import tech.techparterns.identidade.domain.port.Transacao;
import tech.techparterns.identidade.domain.port.Transacao.ViolacaoUnicidadeException;
import tech.techparterns.identidade.domain.repository.EmailRegistradoRepository;
import tech.techparterns.identidade.domain.repository.EmpresaRepository;
import tech.techparterns.identidade.domain.repository.UsuarioEmpresaRepository;
import tech.techparterns.identidade.infrastructure.security.ContextoAutenticado;
import tech.techparterns.shared.domain.Result;
import tech.techparterns.shared.domain.exception.ConflitoException;
import tech.techparterns.shared.domain.repository.UseCase;

/** Dono cria empresa e o primeiro admin PENDING (SETUP de 48 h) numa transação; e-mail em uso → 409 sem criar nada. */
@ApplicationScoped
public class CriarEmpresaUseCase implements UseCase<CriarEmpresaCommand, EmpresaCriadaDto> {

    private final EmpresaRepository empresas;
    private final UsuarioEmpresaRepository usuarios;
    private final EmailRegistradoRepository emails;
    private final NotificadorSenha notificador;
    private final AuditoriaAcesso auditoria;
    private final Transacao transacao;
    private final ContextoAutenticado contexto;
    private final ContextoRequisicao requisicao;

    public CriarEmpresaUseCase(EmpresaRepository empresas, UsuarioEmpresaRepository usuarios,
                               EmailRegistradoRepository emails, NotificadorSenha notificador,
                               AuditoriaAcesso auditoria, Transacao transacao, ContextoAutenticado contexto,
                               ContextoRequisicao requisicao) {
        this.empresas = empresas;
        this.usuarios = usuarios;
        this.emails = emails;
        this.notificador = notificador;
        this.auditoria = auditoria;
        this.transacao = transacao;
        this.contexto = contexto;
        this.requisicao = requisicao;
    }

    @Override
    public Result<EmpresaCriadaDto> execute(CriarEmpresaCommand cmd) {
        var nomeEmpresa = NomeEmpresa.tryCreate(cmd.nome());
        var adminNome = NomeUsuario.tryCreate(cmd.adminNome());
        var adminEmail = EmailUsuario.tryCreate(cmd.adminEmail());
        var telefone = TelefoneUsuario.tryCreateOpcional(cmd.adminTelefone());
        var validacao = Result.combineAll(nomeEmpresa, adminNome, adminEmail, telefone);
        if (validacao.isFailure())
            return Result.propagate(validacao);
        try {
            return Result.ok(transacao.emNovaTransacao(() -> criar(nomeEmpresa.getValue(), adminNome.getValue(),
                    adminEmail.getValue(), telefone.getValue())));
        } catch (ViolacaoUnicidadeException e) {
            throw emailEmUso();
        }
    }

    private EmpresaCriadaDto criar(NomeEmpresa nomeEmpresa, NomeUsuario adminNome, EmailUsuario adminEmail,
                                   TelefoneUsuario telefone) {
        if (emails.emUso(adminEmail))
            throw emailEmUso();
        var empresa = Empresa.tryCreate(nomeEmpresa).getOrThrow();
        empresas.salvar(empresa);
        var admin = UsuarioEmpresa.tryCreate(empresa.getId(), adminNome, adminEmail, telefone, PapelUsuario.ADMIN).getOrThrow();
        usuarios.salvar(admin);
        notificador.notificar(admin, empresa.nome().valor(), TipoTokenSenha.SETUP);
        auditoria.registrar("EMPRESA_CRIADA", AuditoriaAcesso.SUCESSO, TitularTipo.PLATFORM, contexto.userId(),
                empresa.getId(), requisicao.ip(), null);
        return new EmpresaCriadaDto(empresa.getId(), empresa.nome().valor(), empresa.status().name(), admin.getId());
    }

    private static ConflitoException emailEmUso() {
        return new ConflitoException("EMAIL_INDISPONIVEL", ErroIdentidade.EMAIL_INDISPONIVEL.mensagem());
    }
}
