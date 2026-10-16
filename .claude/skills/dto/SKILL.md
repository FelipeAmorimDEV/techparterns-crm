---
name: dto
description: Criar, revisar ou orientar Commands e DTOs no Quarkus (techparterns-crm). Usar quando o pedido envolver `application/command`, `application/dto`, `*Command`, `*Dto`, query de paginação, `@Schema` OpenAPI, ou contrato HTTP sem vazar entidade ou JPA.
---

# DTO / Command

## Overview

`record` na borda da aplicação. Command entra no use case de escrita. DTO sai para a API. Query carrega filtro e página. Nenhum dos três carrega `companyId`: a empresa vem do JWT.

## Guidelines

- Ler `references/dto-pattern.md` antes de criar ou alterar.
- Command em `application/command`, sufixo `Command`.
- DTO em `application/dto`, sufixo `Dto`.
- Query em `application/query`, sufixo `Query`.
- `record` Java.
- Body e resposta levam `@Schema` na classe e em cada campo (`org.eclipse.microprofile.openapi.annotations.media.Schema`).
- Sem regra de domínio, sem JPA, sem entidade.
- Campo opcional usa tipo objeto (`Integer`, `String`), não primitivo.

## Workflow

1. Classificar Command, DTO ou Query.
2. Fechar o contrato mínimo do cliente.
3. Gravar o `record` com `@Schema` se ele aparece no OpenAPI.
4. Conferir o use case e o resource: Command não entra na resposta; DTO não entra no `tryCreate`.

## References

Consultar `references/dto-pattern.md`.
