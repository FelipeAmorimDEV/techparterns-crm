package tech.techparterns.funil.application.usecase;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import tech.techparterns.funil.application.command.CadastrarMotivoPerdaCommand;
import tech.techparterns.funil.domain.model.MotivoPerda;
import tech.techparterns.funil.domain.repository.MotivoPerdaRepository;
import tech.techparterns.shared.domain.CompanyId;
import tech.techparterns.shared.domain.Result;
import tech.techparterns.shared.domain.repository.UseCase;
import tech.techparterns.shared.infrastructure.security.TenantContext;

@ApplicationScoped
public class CadastrarMotivoPerdaUseCase implements UseCase<CadastrarMotivoPerdaCommand, UUID> {

    public static final String ATIVO_OBRIGATORIO = "O indicador de ativo do motivo de perda é obrigatório";
    public static final String NOME_REPETIDO = "Já existe motivo de perda com esse nome nesta empresa";

    private final MotivoPerdaRepository motivos;
    private final TenantContext tenant;

    public CadastrarMotivoPerdaUseCase(MotivoPerdaRepository motivos, TenantContext tenant) {
        this.motivos = motivos;
        this.tenant = tenant;
    }

    @Override
    @Transactional
    public Result<UUID> execute(CadastrarMotivoPerdaCommand cmd) {
        var empresa = CompanyId.reconstituir(tenant.requireCompanyId());
        if (cmd == null || cmd.ativo() == null)
            return Result.fail(ATIVO_OBRIGATORIO);
        var criado = MotivoPerda.tryCreate(empresa.valor(), cmd.nome(), cmd.ativo());
        if (criado.isFailure())
            return Result.propagate(criado);
        var motivo = criado.getValue();
        if (motivos.existeNome(empresa, motivo.nome()))
            return Result.fail(NOME_REPETIDO);
        motivos.salvar(motivo);
        return Result.ok(motivo.getId());
    }
}
