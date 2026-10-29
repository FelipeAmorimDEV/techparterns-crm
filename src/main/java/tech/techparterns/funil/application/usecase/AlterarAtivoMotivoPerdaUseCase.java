package tech.techparterns.funil.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import tech.techparterns.funil.application.command.AlterarAtivoMotivoPerdaCommand;
import tech.techparterns.funil.domain.repository.MotivoPerdaRepository;
import tech.techparterns.shared.domain.CompanyId;
import tech.techparterns.shared.domain.Result;
import tech.techparterns.shared.domain.exception.DomainException;
import tech.techparterns.shared.domain.repository.UseCase;
import tech.techparterns.shared.infrastructure.security.TenantContext;

@ApplicationScoped
public class AlterarAtivoMotivoPerdaUseCase implements UseCase<AlterarAtivoMotivoPerdaCommand, Void> {

    public static final String ATIVO_OBRIGATORIO = "O indicador de ativo do motivo de perda é obrigatório";
    public static final String NAO_ENCONTRADO = "Motivo de perda não encontrado";

    private final MotivoPerdaRepository motivos;
    private final TenantContext tenant;

    public AlterarAtivoMotivoPerdaUseCase(MotivoPerdaRepository motivos, TenantContext tenant) {
        this.motivos = motivos;
        this.tenant = tenant;
    }

    @Override
    @Transactional
    public Result<Void> execute(AlterarAtivoMotivoPerdaCommand cmd) {
        var empresa = CompanyId.reconstituir(tenant.requireCompanyId());
        if (cmd == null || cmd.ativo() == null)
            return Result.fail(ATIVO_OBRIGATORIO);
        var motivo = motivos.buscarPorId(cmd.id(), empresa)
                .orElseThrow(() -> new DomainException(NAO_ENCONTRADO));
        motivo.alterarAtivo(cmd.ativo());
        motivos.salvar(motivo);
        return Result.ok();
    }
}
