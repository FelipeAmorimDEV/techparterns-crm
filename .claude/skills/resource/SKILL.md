---
name: resource
description: Criar, revisar ou orientar Resources JAX-RS no Quarkus (techparterns-crm). Usar quando o pedido envolver `*Resource.java` em `infrastructure/rest`, verbo HTTP, OpenAPI, `execute`, `Result`, `DomainException` ou `GlobalExceptionHandler`.
---

# Resource (Controller)

## Overview

Resource traduz HTTP em Command ou Query, chama `execute` e devolve a resposta. Não valida regra de negócio. Falha de `Result` vira `DomainException` para o `GlobalExceptionHandler`. Empresa não entra como parâmetro da rota: o use case lê o JWT.

## Guidelines

- Ler `references/resource-pattern.md` antes de criar ou alterar resources.
- Classe com `@Path`, `@Produces(MediaType.APPLICATION_JSON)`, `@Consumes(MediaType.APPLICATION_JSON)`, `@ApplicationScoped`.
- Use cases no construtor. Sem `@Inject` em campo.
- Comando: se `isFailure()`, `throw new DomainException(resultado.getErrors())`. Senão, 201 com corpo ou 204 sem corpo.
- Leitura: `execute(query).getValue()` e o DTO direto, sem `Response`.
- Sempre `execute`. Não existe `executar`.
- OpenAPI: `@Tag`, `@Operation`, `@APIResponse`, `@Parameter`. `@DefaultValue` em page e perPage.
- Não capturar `DomainException` no resource.
- Não declarar `@QueryParam` nem path de `companyId` / `fazendaId`.

## Mapeamento atual

| Exceção | HTTP | Código |
|---------|------|--------|
| `NotFoundException` | 404 | `ROTA_NAO_ENCONTRADA` |
| `DomainException`, `IllegalArgumentException` | 400 | `REQUISICAO_INVALIDA` |
| JSON inválido (`InvalidFormatException`) | 400 | `REQUISICAO_INVALIDA` |
| Qualquer outra, inclusive `IllegalStateException` de token sem company | 500 | `ERRO_INTERNO` |

O ramo de 401 para tenant ausente está comentado de propósito em `GlobalExceptionHandler`. Não reativar sem pedido explícito. Status 404/409 de domínio ainda não existe: novo status entra em `mapear`, antes do `instanceof DomainException`.

Corpo de erro: `ErroResponse` (`path`, `mensagem`, `codigoErro`, `dataHora`).

## Workflow

1. `@Path` e anotações da classe.
2. Construtor com os use cases.
3. Um método por rota, com o verbo.
4. OpenAPI do status real (400 para `DomainException`).
5. Extrair body, `@PathParam` ou `@QueryParam`.
6. `execute` e o tratamento de `Result` conforme escrita ou leitura.

## References

Consultar `references/resource-pattern.md`.
