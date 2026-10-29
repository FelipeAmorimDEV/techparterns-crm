---
name: query-cqrs
description: Criar, revisar ou orientar leitura CQRS no Quarkus (techparterns-crm). Usar quando o pedido envolver `*Query.java`, `Listar*UseCase`, `Buscar*UseCase`, projeção, paginação `page`/`perPage`, filtro, JPQL de leitura, ou separar lista da API do repositório de escrita.
---

# Query CQRS

## Overview

Leitura devolve DTO. O use case de leitura implementa `UseCase<Query, Dto>` e o método `execute`, com `Result`. A empresa vem de `TenantContext`, não do record de query. Escrita continua no repositório do agregado.

## Guidelines

- Ler `references/query-cqrs-pattern.md` antes de criar ou alterar queries.
- Record de query com `page`, `perPage` e filtros. Sem `companyId` e sem `fazendaId`.
- Use case `@ApplicationScoped`, construtor, sem `@Transactional`.
- `execute` devolve `Result<Dto>`. Não criar método `executar`.
- Projeção pode ser JPQL no repositório de leitura ou método de lista que já devolve DTO. Não devolver entidade de domínio para a API.
- Página: `setFirstResult(page * perPage)` e `setMaxResults(perPage)`, mais um `count`.

## Workflow

1. Confirmar que é leitura.
2. Record de query com filtros e página.
3. DTOs de saída.
4. Use case `execute` lendo `TenantContext.requireCompanyId()`.
5. JPQL ou método de leitura filtrando `companyId` e `deletedAt is null`.
6. Resource `@GET` chama `execute(...).getValue()`.
7. Testar página com dados, página vazia e cada filtro.

## References

Consultar `references/query-cqrs-pattern.md`.
