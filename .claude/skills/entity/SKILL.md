---
name: entity
description: Criar, revisar ou orientar Entidades de domínio no Quarkus (techparterns-crm). Usar quando o pedido envolver "entidade", "entity", arquivos em `domain/model`, `tryCreate`, `reconstituir`, estender `shared.domain.Entity`, `CompanyId`, ou composição com Value Objects.
---

# Entity

## Overview

Entidade de domínio com identidade, auditoria e exclusão lógica herdadas de `tech.techparterns.shared.domain.Entity`. `tryCreate` valida input e devolve `Result`. `reconstituir` restaura o banco sem revalidar. A classe de domínio não depende de framework.

## Guidelines

- Ler `references/entity-pattern.md` antes de criar ou alterar entidades.
- Estender `Entity`. Construtor da subclasse é `private`.
- `tryCreate(primitivos...)` valida os VOs com `Result.combineAll()` e devolve `Result<Entidade>`.
- `reconstituir(...)` recebe a auditoria da base (`id`, `createdAt`, `updatedAt`, `deletedAt`) e VOs já construídos.
- A identidade da raiz é o `UUID` de `getId()`. Não criar um VO de id paralelo para a mesma raiz.
- Isolamento por empresa usa `CompanyId`. O valor nasce em `TenantContext.requireCompanyId()`, não em campo enviado pelo cliente.
- Campos de negócio usam accessor sem `get` (`nome()`). Auditoria permanece `getId()`, `getCreatedAt()`, `getUpdatedAt()`, `getDeletedAt()`.
- Exclusão lógica usa `setDeletedAt` e `restaurar` da base.
- Método de domínio pode lançar `DomainException` quando a invariante quebra. Input inválido volta em `Result.fail`.
- Sem `@ApplicationScoped`, JPA ou Quarkus na classe de domínio.

## Distinção: tryCreate vs reconstituir

| Método | Quando usar | Valida? | Retorna |
|--------|-------------|---------|---------|
| `tryCreate(primitivos...)` | Input do usuário vindo do Command | Sim, via `combineAll` | `Result<Entidade>` |
| `reconstituir(auditoria, VOs...)` | Dados lidos do banco no Mapper | Não | Entidade direto |

`tryCreate` recebe primitivos e constrói os VOs. O mapper chama `reconstituir` com `VO.reconstituir(...)`.

## Workflow

1. Identificar atributos, invariantes e se a raiz é isolada por empresa.
2. Reutilizar VOs existentes; criar só os que faltam.
3. Estender `Entity` com construtor `private` e campos de negócio.
4. Implementar `tryCreate` com `combineAll`. Em sucesso, gerar `UUID.randomUUID()`.
5. Implementar `reconstituir` chamando `super(id, createdAt, updatedAt, deletedAt)`.
6. Expor accessors de negócio e métodos de domínio.
7. Testar criação, erros acumulados, reconstituição e invariantes em `src/test/java`.

## References

Consultar `references/entity-pattern.md`.
