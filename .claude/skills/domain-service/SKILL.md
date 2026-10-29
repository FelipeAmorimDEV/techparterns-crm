---
name: domain-service
description: Criar, revisar ou orientar serviços de domínio no Quarkus (techparterns-crm). Usar quando a regra não couber numa entidade ou VO, ou quando o arquivo for `*Policy`, `*Calculator`, `*Resolver` ou `*Specification` em `domain/service` ou `domain/model`.
---

# Domain Service

## Overview

Regra pura que combina mais de uma entidade ou VO. Fica em `domain/`, sem framework e sem I/O. Use case só chama. Não orquestra transação.

## Guidelines

- Ler `references/domain-service-pattern.md` antes de criar ou alterar.
- Pacote `<modulo>/domain/service`, ou `domain/model` se a regra for íntima de um agregado.
- Sem import de infraestrutura, Jakarta, Quarkus ou JPA.
- Método estático para cálculo determinístico. Instância só com configuração imutável.
- Nome pela regra: `*Policy`, `*Calculator`, `*Resolver`, `*Specification`.
- Parâmetros e retorno são tipos de domínio.
- Sem banco, HTTP, arquivo ou variável de ambiente.

## Workflow

1. Confirmar que a regra não cabe num método da entidade ou do VO.
2. Definir a API mínima.
3. Implementar sem efeito colateral.
4. Chamar a partir do use case.
5. Testar caso normal e borda, sem Quarkus.

## References

Consultar `references/domain-service-pattern.md`.
