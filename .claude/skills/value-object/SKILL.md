---
name: value-object
description: Criar, revisar ou orientar Value Objects no Quarkus (techparterns-crm). Usar quando o pedido envolver "value object", "objeto de valor", "VO", validação e normalização de atributo de domínio, `CompanyId`, ou arquivo novo em `domain/model`.
---

# Value Object

## Overview

Value Object com `class final`, construtor `private`, `tryCreate` devolvendo `Result<VO>` e `reconstituir` para dado já persistido. Criação a partir de input do usuário não lança exceção.

## Guidelines

- Ler `references/vo-pattern.md` antes de criar ou alterar VOs.
- `class final` e construtor `private`. Sem `record`: o compact constructor sempre roda e impede `reconstituir` sem validação.
- `tryCreate` valida e devolve `Result<VO>`.
- `reconstituir` não valida. Só o mapper, ao ler o banco, chama esse método.
- Normalizar dentro de `tryCreate` (`trim`, conversão, default) antes de construir.
- Factory interna (`de`, `zero`, `padrao`) só para dado que o domínio já validou.
- Mensagem exclusiva do VO: `public static final String` na classe, texto em português.
- Mensagem compartilhada: entrada em `tech.techparterns.shared.domain.exception.ErroDominio`. `CompanyId` usa `ErroDominio.EMPRESA_OBRIGATORIA`.
- Sem framework (sem `@ApplicationScoped`, Jakarta ou Quarkus).

## Distinção: tryCreate vs reconstituir vs de()

| Método | Quando usar | Valida? |
|--------|-------------|---------|
| `tryCreate(input)` | Input do usuário vindo do Command | Sim → `Result<VO>` |
| `reconstituir(valor)` | Dado lido do banco no Mapper | Não → VO direto |
| `de(v)` / `zero()` | Operação interna já validada | Não → VO direto |

## Workflow

1. Definir o tipo base e as invariantes.
2. Procurar VO igual em `domain/model` e em `shared/domain` (`CompanyId`).
3. Implementar `class final`, construtor `private`, mensagem e `tryCreate`.
4. Adicionar `reconstituir`.
5. Adicionar `de`, `zero` ou `padrao` quando o domínio precisar.
6. Adicionar cálculo ou comparação quando o domínio precisar.
7. Testar em `src/test/java`.

## References

Consultar `references/vo-pattern.md`.
