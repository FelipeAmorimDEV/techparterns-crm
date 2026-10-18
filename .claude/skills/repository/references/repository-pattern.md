# Repository Pattern (techparterns-crm — Quarkus)

Pacote: `tech.techparterns.<modulo>`.

## Papel

- Persistir e reconstituir o agregado.
- Sem regra de caso de uso e sem regra de domínio.
- Query de tela (DTO, página, filtro) não passa por este contrato. Ver skill `query-cqrs`.

## Pré-requisito de build

O `pom.xml` tem JDBC, Flyway, PostgreSQL e H2. Não tem `quarkus-hibernate-orm`. Ao criar o primeiro repositório, adicionar:

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-hibernate-orm</artifactId>
</dependency>
```

Em `application.properties`: `quarkus.hibernate-orm.database.generation=none`. Tabela nova entra em `src/main/resources/db/migration`.

## Interface

```java
package tech.techparterns.<modulo>.domain.repository;

import java.util.Optional;
import java.util.UUID;

import tech.techparterns.<modulo>.domain.model.Exemplo;
import tech.techparterns.shared.domain.CompanyId;

public interface ExemploRepository {
    Optional<Exemplo> buscarPorId(UUID id, CompanyId companyId);
    void salvar(Exemplo exemplo);
}
```

`listar` / `contar` devolvendo entidade só quando o comando precisa do agregado. Lista de API devolve DTO na query de leitura.

## Implementação

```java
package tech.techparterns.<modulo>.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import tech.techparterns.<modulo>.domain.model.Exemplo;
import tech.techparterns.<modulo>.domain.repository.ExemploRepository;
import tech.techparterns.<modulo>.infrastructure.persistence.mapper.ExemploMapper;
import tech.techparterns.shared.domain.CompanyId;

@ApplicationScoped
public class ExemploRepositoryImpl implements ExemploRepository {

    private final EntityManager em;

    public ExemploRepositoryImpl(EntityManager em) {
        this.em = em;
    }

    @Override
    public Optional<Exemplo> buscarPorId(UUID id, CompanyId companyId) {
        return em.createQuery(
                        "from ExemploEntity e where e.id = :id and e.companyId = :companyId",
                        ExemploEntity.class)
                .setParameter("id", id)
                .setParameter("companyId", companyId.valor())
                .getResultStream()
                .findFirst()
                .map(ExemploMapper::toDomain);
    }

    @Override
    @Transactional
    public void salvar(Exemplo exemplo) {
        em.merge(ExemploMapper.toEntity(exemplo));
    }
}
```

## Mapper

```java
package tech.techparterns.<modulo>.infrastructure.persistence.mapper;

import tech.techparterns.<modulo>.domain.model.Exemplo;
import tech.techparterns.<modulo>.domain.model.NomeExemplo;
import tech.techparterns.<modulo>.infrastructure.persistence.ExemploEntity;
import tech.techparterns.shared.domain.CompanyId;

public final class ExemploMapper {

    private ExemploMapper() {}

    public static Exemplo toDomain(ExemploEntity entity) {
        return Exemplo.reconstituir(
                entity.id,
                entity.createdAt,
                entity.updatedAt,
                entity.deletedAt,
                CompanyId.reconstituir(entity.companyId),
                NomeExemplo.reconstituir(entity.nome));
    }

    public static ExemploEntity toEntity(Exemplo domain) {
        var entity = new ExemploEntity();
        entity.id = domain.getId();
        entity.createdAt = domain.getCreatedAt();
        entity.updatedAt = domain.getUpdatedAt();
        entity.deletedAt = domain.getDeletedAt();
        entity.companyId = domain.companyId().valor();
        entity.nome = domain.nome().valor();
        return entity;
    }
}
```

`ExemploEntity` mora na infraestrutura e não estende `shared.domain.Entity`.

## Checklist

- Interface sem import de `jakarta.persistence`.
- `@ApplicationScoped` e `EntityManager` no construtor.
- `buscarPorId` devolve `Optional` e sempre filtra `companyId`.
- `salvar` usa `merge`.
- JPQL com parâmetro nomeado.
- Filtro opcional só entra na query se `!= null` e, para texto, `!isBlank()`.
- Soft delete: listagem de ativos filtra `deletedAt is null`, salvo pedido explícito de incluir excluídos.

## Armadilhas

- Tipo JPA na interface.
- Devolver `ExemploEntity` para o use case.
- `tryCreate` no mapper.
- `persist` para atualizar um id que já existe.
- Coluna ou parâmetro `fazendaId`. O campo é `companyId`.
- Gerar schema pelo Hibernate.
