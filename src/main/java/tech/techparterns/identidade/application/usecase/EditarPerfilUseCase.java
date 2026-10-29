package tech.techparterns.identidade.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import tech.techparterns.identidade.application.command.EditarPerfilCommand;
import tech.techparterns.identidade.application.dto.PerfilDto;
import tech.techparterns.identidade.domain.model.*;
import tech.techparterns.identidade.domain.repository.UsuarioEmpresaRepository;
import tech.techparterns.identidade.infrastructure.security.ContextoAutenticado;
import tech.techparterns.shared.domain.Result;
import tech.techparterns.shared.domain.exception.RecursoNaoEncontradoException;
import tech.techparterns.shared.domain.repository.UseCase;

/** O usuário edita só nome e telefone; o próprio e-mail não é alterável (DEL-04, RN-IA-28). */
@ApplicationScoped
public class EditarPerfilUseCase implements UseCase<EditarPerfilCommand, PerfilDto> {

    private final UsuarioEmpresaRepository usuarios;
    private final ContextoAutenticado contexto;

    public EditarPerfilUseCase(UsuarioEmpresaRepository usuarios, ContextoAutenticado contexto) {
        this.usuarios = usuarios;
        this.contexto = contexto;
    }

    @Override
    @Transactional
    public Result<PerfilDto> execute(EditarPerfilCommand cmd) {
        var usuario = usuarios.buscarPorId(contexto.userId(), contexto.empresaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException(ErroIdentidade.USUARIO_NAO_ENCONTRADO.mensagem()));
        if (cmd != null && cmd.email() != null && !cmd.email().isBlank()) {
            var novo = EmailUsuario.tryCreate(cmd.email());
            if (novo.isFailure() || !novo.getValue().equals(usuario.email()))
                return Result.fail(ErroIdentidade.EMAIL_PROPRIO_NAO_ALTERAVEL.mensagem());
        }
        NomeUsuario nome = null;
        if (cmd != null && cmd.nome() != null) {
            var r = NomeUsuario.tryCreate(cmd.nome());
            if (r.isFailure()) return Result.propagate(r);
            nome = r.getValue();
        }
        var telefone = TelefoneUsuario.tryCreateOpcional(cmd == null ? null : cmd.telefone());
        if (telefone.isFailure()) return Result.propagate(telefone);
        usuario.editarContato(nome, telefone.getValue());
        usuarios.salvar(usuario);
        return Result.ok(PerfilDto.de(usuario));
    }
}
