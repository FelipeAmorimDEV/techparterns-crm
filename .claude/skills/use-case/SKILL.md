---
name: use-case
description: Criar, revisar ou orientar casos de uso no Quarkus (techparterns-crm). Usar quando o pedido envolver "caso de uso", "use case", `*UseCase.java`, `UseCase<T,R>`, `execute`, `@ApplicationScoped`, `@Transactional`, `TenantContext` ou teste de orquestração.
---

# Use Case

## Overview

Caso de uso orquestra repositório, entidade e VO. Implementa `UseCase<Command, Retorno>` e devolve `Result`. Invariante fica na entidade. O id da empresa sai do JWT, via `TenantContext`.

## Guidelines

- Ler `references/use-case-pattern.md` antes de criar ou alterar use cases.
- Interface `tech.techparterns.shared.domain.repository.UseCase<T, R>`, método `execute`.
- `@ApplicationScoped`. Dependência no construtor, sem `@Inject` em campo.
- `@Transactional` em `execute` quando houver escrita.
- Input do cliente: `VO.tryCreate` + `Result.combineAll` no início. Falha → `Result.propagate`. Sem exceção de validação.
- Empresa: `CompanyId.reconstituir(tenant.requireCompanyId())`. O Command não traz `companyId`.
- `requireCompanyId()` lança `IllegalStateException("Company ausente no token")` se o claim faltar. O `GlobalExceptionHandler` não mapeia isso para 401; o mapeamento está suspenso.
- Entidade nova: `Entidade.tryCreate(...)`. Falha propaga com `Result.propagate`.
- Invariante (`DomainException`) segue sem catch. O handler HTTP responde 400.
- Sem tipo de JPA, Panache ou Hibernate no use case.
- Dado de outro módulo entra por port no módulo consumidor (`<modulo>/domain/port`). A implementação mora no módulo dono. Não colocar esse port em `shared`.

## Result vs Exception

| Situação | Mecanismo |
|---|---|
| Input inválido | `Result.fail` / `Result.propagate` |
| Invariante do agregado (não encontrado, estado ilegal) | `DomainException` (subclasse do módulo), sem catch |

Hoje o handler HTTP responde 400 para qualquer `DomainException`. Status 404 ou 409 pede ramo novo no handler, antes do ramo genérico.

## Workflow

1. Definir ou reutilizar o `Command` em `application/command`.
2. Listar repositório, port, `TenantContext` e serviço de domínio.
3. Classe `@ApplicationScoped` com construtor.
4. `execute`: empresa pelo tenant → validar input → criar ou buscar → comportamento → `salvar` → `Result.ok`.
5. Não duplicar invariante que a entidade já garante.
6. Escrita múltipla no mesmo `@Transactional`.
7. Testar feliz, validação e invariante. Fake de repositório, sem subir o Quarkus.

## References

Consultar `references/use-case-pattern.md`.
