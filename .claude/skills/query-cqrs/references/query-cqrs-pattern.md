# Query CQRS Pattern (techparterns-crm — Quarkus)

## Quando é query

- A API precisa de uma projeção, página, filtro ou agregação.
- O cliente não vai chamar método de domínio em cima do resultado.

## Quando é repositório

- Criar, atualizar, excluir.
- Carregar o agregado para um comando (`buscarPorId` → entidade).

## Objetos

```java
package tech.techparterns.<modulo>.application.query;

public record ListarExemplosQuery(int page, int perPage, String nomeFiltro) {
}
```

```java
package tech.techparterns.<modulo>.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;

import tech.techparterns.<modulo>.application.dto.ExemploListaDto;
import tech.techparterns.<modulo>.application.query.ListarExemplosQuery;
import tech.techparterns.<modulo>.domain.repository.ExemploConsulta;
import tech.techparterns.shared.domain.CompanyId;
import tech.techparterns.shared.domain.Result;
import tech.techparterns.shared.domain.repository.UseCase;
import tech.techparterns.shared.infrastructure.security.TenantContext;

@ApplicationScoped
public class ListarExemplosUseCase implements UseCase<ListarExemplosQuery, ExemploListaDto> {

    private final ExemploConsulta consulta;
    private final TenantContext tenant;

    public ListarExemplosUseCase(ExemploConsulta consulta, TenantContext tenant) {
        this.consulta = consulta;
        this.tenant = tenant;
    }

    @Override
    public Result<ExemploListaDto> execute(ListarExemplosQuery query) {
        var companyId = CompanyId.reconstituir(tenant.requireCompanyId());
        return Result.ok(consulta.listar(companyId, query));
    }
}
```

`ExemploConsulta` pode ser a mesma implementação física do repositório, com um método que devolve DTO. O use case de escrita não usa esse método para decidir invariante.

## JPQL

Filtro opcional só é concatenado se o valor veio. Texto: `lower(e.nome) like :nome` com `%termo%` em minúsculas. Sempre `e.companyId = :companyId`. Lista padrão: `e.deletedAt is null`.

Paginação: `setFirstResult(query.page() * query.perPage())`, `setMaxResults(query.perPage())`. O `total` vem de um `count` com os mesmos filtros, sem `firstResult`.

`new CompanyId(...)` não compila: construtor privado. Usar `CompanyId.reconstituir`.

## Checklist

- Use case implementa `UseCase` e `execute`.
- Sem `@Transactional`.
- DTO não estende `Entity`.
- `companyId` do tenant, em toda query.
- `count` separado.

## Armadilhas

- Método `executar` devolvendo DTO nu. O resource deste projeto chama `execute` e `getValue()`.
- `companyId` em query string. Qualquer cliente passaria a ler outra empresa.
- Reutilizar o use case de escrita para a listagem.
- JPQL com filtro opcional sem o `if`, gerando parâmetro ausente.
- `total` igual ao tamanho da página.
