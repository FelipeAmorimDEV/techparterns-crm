package tech.techparterns.identidade.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import tech.techparterns.identidade.application.command.SemearDonoCommand;
import tech.techparterns.identidade.domain.model.DonoPlataforma;
import tech.techparterns.identidade.domain.model.EmailUsuario;
import tech.techparterns.identidade.domain.model.NomeUsuario;
import tech.techparterns.identidade.domain.repository.DonoPlataformaRepository;
import tech.techparterns.identidade.domain.repository.UsuarioEmpresaRepository;
import tech.techparterns.shared.domain.Result;
import tech.techparterns.shared.domain.repository.UseCase;

/** Semente do primeiro dono: só se não existe nenhum; idempotente; sem senha (RN-IA-27, CA-24). */
@ApplicationScoped
public class SemearDonoUseCase implements UseCase<SemearDonoCommand, SemearDonoUseCase.Desfecho> {

    public enum Desfecho { CRIADO, JA_EXISTE }

    private final DonoPlataformaRepository donos;
    private final UsuarioEmpresaRepository usuarios;

    public SemearDonoUseCase(DonoPlataformaRepository donos, UsuarioEmpresaRepository usuarios) {
        this.donos = donos;
        this.usuarios = usuarios;
    }

    @Override
    @Transactional
    public Result<Desfecho> execute(SemearDonoCommand cmd) {
        if (donos.existeAlgum())
            return Result.ok(Desfecho.JA_EXISTE);
        if (cmd.email() == null || cmd.email().isBlank() || cmd.nome() == null || cmd.nome().isBlank())
            return Result.fail("CRM_OWNER_EMAIL e CRM_OWNER_NOME são obrigatórios para criar o dono da plataforma");
        var email = EmailUsuario.tryCreate(cmd.email());
        var nome = NomeUsuario.tryCreate(cmd.nome());
        var validacao = Result.combineAll(email, nome);
        if (validacao.isFailure())
            return Result.propagate(validacao);
        if (usuarios.existeEmail(email.getValue()))
            return Result.fail("O e-mail do dono já está em uso por um usuário de empresa");
        donos.salvar(DonoPlataforma.tryCreate(nome.getValue(), email.getValue()).getOrThrow());
        return Result.ok(Desfecho.CRIADO);
    }
}
