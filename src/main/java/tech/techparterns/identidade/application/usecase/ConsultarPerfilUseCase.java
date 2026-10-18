package tech.techparterns.identidade.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;

import tech.techparterns.identidade.application.dto.PerfilDto;
import tech.techparterns.identidade.application.query.ConsultarPerfilQuery;
import tech.techparterns.identidade.domain.model.ErroIdentidade;
import tech.techparterns.identidade.domain.repository.UsuarioEmpresaRepository;
import tech.techparterns.identidade.infrastructure.security.ContextoAutenticado;
import tech.techparterns.shared.domain.Result;
import tech.techparterns.shared.domain.exception.RecursoNaoEncontradoException;
import tech.techparterns.shared.domain.repository.UseCase;

@ApplicationScoped
public class ConsultarPerfilUseCase implements UseCase<ConsultarPerfilQuery, PerfilDto> {

    private final UsuarioEmpresaRepository usuarios;
    private final ContextoAutenticado contexto;

    public ConsultarPerfilUseCase(UsuarioEmpresaRepository usuarios, ContextoAutenticado contexto) {
        this.usuarios = usuarios;
        this.contexto = contexto;
    }

    @Override
    public Result<PerfilDto> execute(ConsultarPerfilQuery query) {
        var usuario = usuarios.buscarPorId(contexto.userId(), contexto.empresaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException(ErroIdentidade.USUARIO_NAO_ENCONTRADO.mensagem()));
        return Result.ok(PerfilDto.de(usuario));
    }
}
