package tech.techparterns.identidade.application.usecase;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

import tech.techparterns.identidade.application.dto.ConviteListadoDto;
import tech.techparterns.identidade.application.query.ListarConvitesQuery;
import tech.techparterns.identidade.domain.repository.ConviteRepository;
import tech.techparterns.identidade.infrastructure.security.ContextoAutenticado;
import tech.techparterns.shared.domain.Result;
import tech.techparterns.shared.domain.repository.UseCase;

@ApplicationScoped
public class ListarConvitesUseCase implements UseCase<ListarConvitesQuery, List<ConviteListadoDto>> {

    private final ConviteRepository convites;
    private final ContextoAutenticado contexto;

    public ListarConvitesUseCase(ConviteRepository convites, ContextoAutenticado contexto) {
        this.convites = convites;
        this.contexto = contexto;
    }

    @Override
    public Result<List<ConviteListadoDto>> execute(ListarConvitesQuery query) {
        return Result.ok(convites.listar(contexto.empresaId()).stream().map(ConviteListadoDto::de).toList());
    }
}
