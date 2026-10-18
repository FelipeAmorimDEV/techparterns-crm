package tech.techparterns.identidade.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;

import tech.techparterns.identidade.application.dto.PaginaDto;
import tech.techparterns.identidade.application.dto.UsuarioListadoDto;
import tech.techparterns.identidade.application.query.ListarUsuariosQuery;
import tech.techparterns.identidade.domain.model.*;
import tech.techparterns.identidade.domain.repository.EnvioEmailRepository;
import tech.techparterns.identidade.domain.repository.UsuarioEmpresaRepository;
import tech.techparterns.identidade.infrastructure.security.ContextoAutenticado;
import tech.techparterns.shared.domain.Result;
import tech.techparterns.shared.domain.repository.UseCase;

/** Leitura (CQRS): sem {@code @Transactional}. A empresa vem sempre do token (RF-IA-22). */
@ApplicationScoped
public class ListarUsuariosUseCase implements UseCase<ListarUsuariosQuery, PaginaDto<UsuarioListadoDto>> {

    private final UsuarioEmpresaRepository usuarios;
    private final EnvioEmailRepository envios;
    private final ContextoAutenticado contexto;

    public ListarUsuariosUseCase(UsuarioEmpresaRepository usuarios, EnvioEmailRepository envios,
                                 ContextoAutenticado contexto) {
        this.usuarios = usuarios;
        this.envios = envios;
        this.contexto = contexto;
    }

    @Override
    public Result<PaginaDto<UsuarioListadoDto>> execute(ListarUsuariosQuery query) {
        int page = query.page() == null ? 0 : query.page();
        int size = query.size() == null ? 20 : query.size();
        if (page < 0 || size < 1 || size > 100)
            return Result.fail(ErroIdentidade.PAGINACAO_INVALIDA.mensagem());

        StatusUsuario status = null;
        if (query.status() != null && !query.status().isBlank()) {
            try {
                status = StatusUsuario.valueOf(query.status().trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                return Result.fail(ErroIdentidade.STATUS_INVALIDO.mensagem());
            }
        }
        PapelUsuario papel = null;
        if (query.role() != null && !query.role().isBlank()) {
            var r = PapelParser.parse(query.role(), null);
            if (r.isFailure()) return Result.propagate(r);
            papel = r.getValue();
        }
        var empresaId = contexto.empresaId();
        var itens = usuarios.listar(empresaId, status, papel, page, size).stream()
                .map(u -> UsuarioListadoDto.de(u,
                        u.status() == StatusUsuario.PENDING ? envios.ultimoStatusDoUsuario(u.getId()).orElse(null) : null))
                .toList();
        return Result.ok(new PaginaDto<>(itens, page, size, usuarios.contar(empresaId, status, papel)));
    }
}
