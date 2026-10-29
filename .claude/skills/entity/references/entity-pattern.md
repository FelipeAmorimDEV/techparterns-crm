# Entity Pattern (techparterns-crm — Quarkus)

Pacote raiz: `tech.techparterns.<modulo>`. Núcleo: `tech.techparterns.shared`.

## Paths

- Base: `shared/domain/Entity.java` — `UUID`, `createdAt`, `updatedAt`, `deletedAt`.
- Empresa: `shared/domain/CompanyId.java`.
- Resultado: `shared/domain/Result.java`.
- Exceção de invariante: `shared/domain/exception/DomainException.java`.
- Mensagens compartilhadas: `shared/domain/exception/ErroDominio.java`.
- Entidade do módulo: `<modulo>/domain/model/Xxx.java`.

## Estrutura Esperada

1. `class Xxx extends Entity` com construtor `private`.
2. `CompanyId companyId` quando o agregado é da empresa. Sem `fazendaId`.
3. Campos de negócio mutáveis só quando há transição de estado.
4. `static Result<Xxx> tryCreate(primitivos...)` valida com `combineAll`.
5. `static Xxx reconstituir(...)` não valida.
6. Negócio: `nome()`. Base: `getId()`, `getCreatedAt()`, `getUpdatedAt()`, `getDeletedAt()`, `isDeleted()`.
7. `getUpdatedAt()` guarda o instante da construção ou da reconstituição. Método de domínio não o altera.
8. Listas internas do agregado ficam encapsuladas.

## Checklist

- `tryCreate` chama `VO.tryCreate` e `CompanyId.tryCreate`, depois `Result.combineAll`.
- Falha: `return Result.propagate(validacao)`.
- Sucesso: `return Result.ok(new Xxx(UUID.randomUUID(), ...))`, que chama `super(id)`.
- `reconstituir` recebe VOs de `VO.reconstituir` e delega ao construtor que chama `super(id, createdAt, updatedAt, deletedAt)`.
- Invariante em método de domínio pode lançar `DomainException`. Input inválido não lança.
- Nenhum `String` cru em atributo que tem regra.
- Nenhuma anotação JPA, CDI ou Quarkus.

## Exemplo Mínimo

```java
package tech.techparterns.<modulo>.domain.model;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

import tech.techparterns.shared.domain.CompanyId;
import tech.techparterns.shared.domain.Entity;
import tech.techparterns.shared.domain.Result;
import tech.techparterns.shared.domain.exception.DomainException;

public class Exemplo extends Entity {

    private final CompanyId companyId;
    private NomeExemplo nome;

    private Exemplo(UUID id, CompanyId companyId, NomeExemplo nome) {
        super(id);
        this.companyId = companyId;
        this.nome = nome;
    }

    private Exemplo(UUID id, Instant createdAt, Instant updatedAt, OffsetDateTime deletedAt,
                    CompanyId companyId, NomeExemplo nome) {
        super(id, createdAt, updatedAt, deletedAt);
        this.companyId = companyId;
        this.nome = nome;
    }

    public static Result<Exemplo> tryCreate(String companyIdStr, String nomeStr) {
        var companyIdResult = CompanyId.tryCreate(companyIdStr);
        var nomeResult = NomeExemplo.tryCreate(nomeStr);
        var validacao = Result.combineAll(companyIdResult, nomeResult);
        if (validacao.isFailure())
            return Result.propagate(validacao);
        return Result.ok(new Exemplo(
                UUID.randomUUID(),
                companyIdResult.getValue(),
                nomeResult.getValue()));
    }

    public static Exemplo reconstituir(UUID id, Instant createdAt, Instant updatedAt,
                                       OffsetDateTime deletedAt, CompanyId companyId, NomeExemplo nome) {
        return new Exemplo(id, createdAt, updatedAt, deletedAt, companyId, nome);
    }

    public void renomear(NomeExemplo novoNome) {
        if (novoNome == null)
            throw new DomainException("Nome não pode ser nulo");
        this.nome = novoNome;
    }

    public CompanyId companyId() { return companyId; }
    public NomeExemplo nome() { return nome; }
}
```

## Exceções de invariante

Subclasse em `<modulo>/domain/exception`, estendendo `DomainException`, só para invariante.

`GlobalExceptionHandler` trata toda `DomainException` como HTTP 400 `REQUISICAO_INVALIDA`. Outro status exige um ramo novo em `mapear`, antes do ramo genérico de `DomainException`.

## Testes

- `tryCreate` válido → `isOk()`, `nome().valor()` e `getId()` preenchidos.
- Cada campo inválido → `isFailure()` com a mensagem do VO ou de `ErroDominio`.
- Vários campos inválidos → todos os erros no mesmo `Result`.
- `reconstituir` aceita o valor persistido e preserva `getCreatedAt()` / `getDeletedAt()`.
- Método de domínio: estado antes e depois; invariante lança `DomainException`.

## Armadilhas

- `criar(VOs...)` no lugar de `tryCreate(primitivos...)`.
- `VO.tryCreate` no mapper. O mapper usa `reconstituir`.
- Validar regra de negócio no use case em vez da entidade.
- Anotação JPA na classe de domínio.
- `Result.attempt` na criação: mistura os erros dos campos.
- Conferir um `Result` por vez e devolver no primeiro erro. Usar `combineAll`.
- Inventar `XxxId` em `String` para a raiz. O id da raiz já é `getId()`.
- Pedir `companyId` no body. Ele vem do token, no use case.
