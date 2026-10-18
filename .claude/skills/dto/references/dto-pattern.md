# DTO / Command Pattern (techparterns-crm — Quarkus)

Pacote: `tech.techparterns.<modulo>.application`.

| Tipo | Pasta | Sufixo | Exemplo |
|------|-------|--------|---------|
| Entrada de escrita | `command` | `Command` | `CriarExemploCommand` |
| Saída | `dto` | `Dto` | `ExemploResponseDto`, `ExemploListaDto` |
| Parâmetro de leitura | `query` | `Query` | `ListarExemplosQuery` |

## Command

```java
package tech.techparterns.<modulo>.application.command;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "CriarExemploRequest", description = "Dados para criar um exemplo.")
public record CriarExemploCommand(
        @Schema(description = "Nome do exemplo.")
        String nome) {
}
```

Sem `companyId` e sem `fazendaId`. `TenantContext` lê o claim `companyId`.

## Resposta

```java
package tech.techparterns.<modulo>.application.dto;

import java.util.UUID;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "CriarExemploResponse", description = "Identificador do exemplo criado.")
public record ExemploResponseDto(
        @Schema(description = "Identificador.")
        UUID id) {
}
```

## Lista

```java
package tech.techparterns.<modulo>.application.dto;

import java.util.List;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "ExemploLista", description = "Lista paginada de exemplos.")
public record ExemploListaDto(
        @Schema(description = "Itens da página.")
        List<LinhaExemploDto> itens,
        @Schema(description = "Total de registros, sem paginação.")
        long total) {
}
```

## Query

```java
package tech.techparterns.<modulo>.application.query;

public record ListarExemplosQuery(int page, int perPage, String nome) {
}
```

Query de página inclui `page` e `perPage`. Não inclui `companyId`. `@Schema` na query só se o record for body; query string documenta-se no resource com `@Parameter`.

## Fronteiras

- Command, DTO e Query não conhecem entidade nem JPA.
- Formatação de saída acontece no use case, ao montar o DTO.
- Opcional é wrapper. Primitivo não aceita ausência.

## Checklist

- Nome com sufixo de intenção.
- `@Schema` na classe e nos campos expostos no OpenAPI.
- Sem `companyId` vindo do cliente.
- Sem acoplamento a tabela ou entidade.

## Armadilhas

- Entidade de domínio como body ou resposta.
- Primitivo em campo opcional.
- `@Schema` só na classe.
- Um record servindo de Command e de resposta.
- Copiar coluna do banco para o nome do campo público sem necessidade do cliente.
