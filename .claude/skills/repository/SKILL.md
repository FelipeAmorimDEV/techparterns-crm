---
name: repository
description: Criar, revisar ou orientar repositórios no Quarkus (techparterns-crm). Usar quando o pedido envolver `*Repository.java` em `domain/repository`, `*RepositoryImpl.java`, `EntityManager`, mapper `toDomain`/`toEntity`, Flyway, ou teste de persistência.
---

# Repository

## Overview

Contrato no domínio, implementação na infraestrutura com JPA `EntityManager`. O mapper converte a entidade de domínio (subclasse de `shared.domain.Entity`) e a entidade JPA. Leitura de projeção para a API fica na query CQRS, não aqui.

## Guidelines

- Ler `references/repository-pattern.md` antes de criar ou alterar repositórios.
- Interface em `<modulo>/domain/repository`, sem tipo de JPA.
- Implementação em `<modulo>/infrastructure/persistence`, `@ApplicationScoped`, `EntityManager` no construtor.
- Mapper em `infrastructure/persistence/mapper`, métodos estáticos `toDomain` e `toEntity`.
- `toDomain` usa `reconstituir`. Nunca `tryCreate` nem `new` de VO.
- `buscarPorId` devolve `Optional` e filtra por `getId()` e `companyId`.
- `quarkus-hibernate-orm` ainda não está no `pom.xml`. Incluir a dependência ao criar o primeiro repositório.
- Schema em Flyway (`src/main/resources/db/migration`). Hibernate não gera DDL.
- Operação composta (agregado e filhos) na mesma transação.

## Workflow

1. Confirmar o agregado e se ele é isolado por `CompanyId`.
2. Declarar a interface com o mínimo de métodos.
3. Criar a entidade JPA em `infrastructure/persistence` (classe diferente da de domínio).
4. Criar o mapper.
5. Implementar `XxxRepositoryImpl`.
6. Migration Flyway para a tabela.
7. Fake em memória para teste de use case. Teste com banco só para o repositório.

## References

Consultar `references/repository-pattern.md`.
