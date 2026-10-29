package tech.techparterns.lead.application.usecase;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import tech.techparterns.lead.application.command.ImportarLeadsCommand;
import tech.techparterns.lead.application.csv.CsvLeads;
import tech.techparterns.lead.application.csv.ValidadorLinhaLead;
import tech.techparterns.lead.application.dto.ImportacaoLeadDto;
import tech.techparterns.lead.application.dto.LinhaRejeitadaDto;
import tech.techparterns.lead.domain.model.Lead;
import tech.techparterns.lead.domain.port.DistribuirLeadNovo;
import tech.techparterns.lead.domain.port.EntrarLeadNoFunil;
import tech.techparterns.lead.domain.repository.LeadRepository;
import tech.techparterns.lead.domain.service.DuplicidadeLeadResolver;
import tech.techparterns.shared.domain.CompanyId;
import tech.techparterns.shared.domain.Result;
import tech.techparterns.shared.domain.exception.DomainException;
import tech.techparterns.shared.domain.exception.ErroDominio;
import tech.techparterns.shared.domain.repository.UseCase;
import tech.techparterns.shared.infrastructure.security.TenantContext;

@ApplicationScoped
public class ImportarLeadsUseCase implements UseCase<ImportarLeadsCommand, ImportacaoLeadDto> {

    public static final String PAPEL_ADMIN = "admin";
    public static final String ACAO_IGNORAR = "ignorar";
    public static final String ACAO_COMPLETAR = "completar_campos_vazios";
    public static final String ACAO_CRIAR_MESMO_ASSIM = "criar_mesmo_assim";
    public static final String ACAO_INVALIDA = "Ação de importação inválida";
    public static final String ARQUIVO_GRANDE = "Arquivo acima do tamanho permitido";
    public static final int TAMANHO_MAXIMO = 10 * 1024 * 1024;

    private final LeadRepository repository;
    private final TenantContext tenant;
    private final Clock clock;
    private final DistribuirLeadNovo distribuirLeadNovo;
    private final EntrarLeadNoFunil entrarLeadNoFunil;

    public ImportarLeadsUseCase(LeadRepository repository, TenantContext tenant, Clock clock,
                                DistribuirLeadNovo distribuirLeadNovo, EntrarLeadNoFunil entrarLeadNoFunil) {
        this.repository = repository;
        this.tenant = tenant;
        this.clock = clock;
        this.distribuirLeadNovo = distribuirLeadNovo;
        this.entrarLeadNoFunil = entrarLeadNoFunil;
    }

    @Override
    @Transactional
    public Result<ImportacaoLeadDto> execute(ImportarLeadsCommand cmd) {
        if (tenant.role() == null || !PAPEL_ADMIN.equalsIgnoreCase(tenant.role()))
            throw new DomainException(ErroDominio.OPERACAO_RESTRITA_AO_ADMIN.mensagem());
        var empresa = CompanyId.reconstituir(tenant.requireCompanyId());

        String acao = cmd.acao();
        if (!ACAO_IGNORAR.equals(acao) && !ACAO_COMPLETAR.equals(acao) && !ACAO_CRIAR_MESMO_ASSIM.equals(acao))
            throw new DomainException(ACAO_INVALIDA);
        byte[] arquivo = cmd.arquivo() == null ? new byte[0] : cmd.arquivo();
        if (arquivo.length > TAMANHO_MAXIMO)
            throw new DomainException(ARQUIVO_GRANDE);
        var lidas = CsvLeads.ler(arquivo);
        if (lidas.isFailure())
            throw new DomainException(lidas.getErrors());

        var ativos = new ArrayList<>(repository.listarNaoExcluidos(empresa));
        var rejeitadas = new ArrayList<LinhaRejeitadaDto>();
        int criados = 0, ignorados = 0, completados = 0, criadosMesmoAssim = 0;

        for (var linha : lidas.getValue()) {
            var validada = ValidadorLinhaLead.validar(linha);
            if (validada.isFailure()) {
                rejeitadas.add(new LinhaRejeitadaDto(linha.numero(), String.join("; ", validada.getErrors())));
                continue;
            }
            var v = validada.getValue();
            var encontrado = DuplicidadeLeadResolver.resolver(v.telefone(), v.email(), ativos);

            if (encontrado.isEmpty() || ACAO_CRIAR_MESMO_ASSIM.equals(acao)) {
                var criado = Lead.tryCreate(empresa, v.nome(), v.telefone(), v.whatsapp(), v.email());
                if (criado.isFailure())
                    throw new DomainException(criado.getErrors());
                repository.salvar(criado.getValue());
                distribuirLeadNovo.aoCriar(criado.getValue());
                entrarLeadNoFunil.entrar(criado.getValue());
                ativos.add(criado.getValue());
                if (encontrado.isEmpty()) criados++; else criadosMesmoAssim++;
            } else if (ACAO_IGNORAR.equals(acao)) {
                ignorados++;
            } else {
                var atual = encontrado.get();
                var novo = atual.completarMeiosVazios(v.telefone(), v.whatsapp(), v.email(), clock.instant());
                if (novo != atual) {
                    repository.salvar(novo);
                    ativos.set(ativos.indexOf(atual), novo);
                }
                completados++;
            }
        }

        return Result.ok(new ImportacaoLeadDto(criados, ignorados, completados, criadosMesmoAssim,
                rejeitadas.size(), List.copyOf(rejeitadas)));
    }
}
