# Use Case Pattern (techparterns-crm — Quarkus)

## Paths

- Contrato: `shared/domain/repository/UseCase.java` — `Result<R> execute(T input)`.
- Tenant: `shared/infrastructure/security/TenantContext.java` — claim `companyId`, métodos `requireCompanyId()` e `requireUserId()`.
- Empresa: `shared/domain/CompanyId.java`.
- Use case do módulo: `<modulo>/application/usecase/*UseCase.java`.
- Command: `<modulo>/application/command/*Command.java`.
- Query de leitura: `<modulo>/application/query/*Query.java`. O use case de leitura também implementa `UseCase` e `execute`. Não existe método `executar`.

## Retorno

- Escrita: `Result<UUID>` com `exemplo.getId()`, ou `Result<Void>` quando não há corpo.
- Leitura: `Result<Dto>`.

## Checklist

- `@ApplicationScoped`.
- `implements UseCase<Command, Retorno>`.
- Construtor com todas as dependências.
- `@Transactional` só na escrita.
- `combineAll` antes de qualquer efeito.
- Falha de input via `Result.propagate`.
- `CompanyId` a partir de `tenant.requireCompanyId()`, com `reconstituir` (o token já foi exigido).
- Sem import de JPA.

## Escrita

```java
package tech.techparterns.<modulo>.application.usecase;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import tech.techparterns.<modulo>.application.command.CriarExemploCommand;
import tech.techparterns.<modulo>.domain.model.Exemplo;
import tech.techparterns.<modulo>.domain.repository.ExemploRepository;
import tech.techparterns.shared.domain.CompanyId;
import tech.techparterns.shared.domain.Result;
import tech.techparterns.shared.domain.repository.UseCase;
import tech.techparterns.shared.infrastructure.security.TenantContext;

@ApplicationScoped
public class CriarExemploUseCase implements UseCase<CriarExemploCommand, UUID> {

    private final ExemploRepository exemplos;
    private final TenantContext tenant;

    public CriarExemploUseCase(ExemploRepository exemplos, TenantContext tenant) {
        this.exemplos = exemplos;
        this.tenant = tenant;
    }

    @Override
    @Transactional
    public Result<UUID> execute(CriarExemploCommand cmd) {
        var companyId = CompanyId.reconstituir(tenant.requireCompanyId());
        var resultado = Exemplo.tryCreate(companyId.valor(), cmd.nome());
        if (resultado.isFailure())
            return Result.propagate(resultado);
        var exemplo = resultado.getValue();
        exemplos.salvar(exemplo);
        return Result.ok(exemplo.getId());
    }
}
```

`TenantContext` é `@RequestScoped`. O CDI injeta o proxy no `@ApplicationScoped`.

## Leitura

```java
@Override
public Result<ExemploListaDto> execute(ListarExemplosQuery query) {
    var companyId = CompanyId.reconstituir(tenant.requireCompanyId());
    var itens = exemplos.listar(companyId, query.page(), query.perPage(), query.nome());
    var total = exemplos.contar(companyId, query.nome());
    return Result.ok(new ExemploListaDto(/* projeção */, total));
}
```

Leitura não leva `@Transactional`. O `companyId` da query interna também sai do tenant, não do record vindo do cliente.

## Relógio

Regra que depende de "hoje" ou de instante usa o `Clock` produzido por `shared/infrastructure/config/RelogioProducer` (zona `America/Sao_Paulo`). Data civil de Brasília: `shared/time/Fusos`. Não chamar `LocalDate.now()` sem zona.

## Resource

```java
var resultado = criarExemplo.execute(cmd);
if (resultado.isFailure())
    throw new DomainException(resultado.getErrors());
return Response.status(Response.Status.CREATED).entity(resultado.getValue()).build();
```

Leitura: `listar.execute(query).getValue()` — `getValue()` em `Result` falho lança `IllegalStateException`.

## Testes

- Command válido grava e devolve `isOk()` com o `UUID`.
- Nome vazio devolve `isFailure()` e a mensagem do VO. Repositório não é chamado.
- Invariante esperada: `assertThrows(DomainException.class, ...)`.
- Montar `TenantContext` é difícil fora do JWT. Nos testes de use case, extrair a obtenção do `CompanyId` para um colaborador pequeno se o construtor do contexto atrapalhar — ou passar um fake do mesmo tipo se o teste subir o CDI. Preferir teste sem `@QuarkusTest`.

## Armadilhas

- Revalidar na mão o que o VO já valida.
- `@Inject` em campo.
- `@Transactional` ausente quando há mais de uma escrita.
- Devolver a entidade no `Result`.
- Engolir `DomainException`.
- `companyId` ou `fazendaId` no Command.
- Método `executar`. O contrato é `execute`.
- Importar o repositório de outro módulo. Criar um port.
