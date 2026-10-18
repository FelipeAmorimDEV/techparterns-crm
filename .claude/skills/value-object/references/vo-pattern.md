# VO Pattern (techparterns-crm — Quarkus)

Pacote do módulo: `tech.techparterns.<modulo>.domain.model`.
Compartilhado: `tech.techparterns.shared.domain`.

## Referência real

`shared/domain/CompanyId.java` — `tryCreate` / `reconstituir`, mensagem em `ErroDominio.EMPRESA_OBRIGATORIA`. Identificador da empresa vem do claim `companyId` do JWT (`TenantContext`), não de um id de fazenda.

## Princípios

- Campo `private final`, sem setter.
- Construtor privado. Código externo não dá `new`.
- `tryCreate` devolve `Result`. Não lança para input inválido.
- `reconstituir` confia no que já foi gravado.
- Normalizar no `tryCreate`.
- Texto da mensagem é frase em português, não código (`"NOME_OBRIGATORIO"` é o formato errado).

## Skeleton — String

```java
package tech.techparterns.<modulo>.domain.model;

import tech.techparterns.shared.domain.Result;

public final class NomeExemplo {

    public static final String OBRIGATORIO = "Nome é obrigatório";

    private final String valor;

    private NomeExemplo(String valor) {
        this.valor = valor;
    }

    public static Result<NomeExemplo> tryCreate(String valor) {
        if (valor == null || valor.isBlank())
            return Result.fail(OBRIGATORIO);
        return Result.ok(new NomeExemplo(valor.trim()));
    }

    public static NomeExemplo reconstituir(String valor) {
        return new NomeExemplo(valor);
    }

    public String valor() { return valor; }

    @Override
    public String toString() { return valor; }
}
```

## Skeleton — BigDecimal

```java
package tech.techparterns.<modulo>.domain.model;

import java.math.BigDecimal;

import tech.techparterns.shared.domain.Result;

public final class ExemploValor {

    public static final String INVALIDO = "Valor não pode ser negativo";

    private final BigDecimal valor;

    private ExemploValor(BigDecimal valor) {
        this.valor = valor;
    }

    public static Result<ExemploValor> tryCreate(BigDecimal v) {
        if (v == null || v.signum() < 0)
            return Result.fail(INVALIDO);
        return Result.ok(new ExemploValor(v));
    }

    public static ExemploValor de(BigDecimal v) {
        return new ExemploValor(v);
    }

    public static ExemploValor zero() {
        return new ExemploValor(BigDecimal.ZERO);
    }

    public BigDecimal valor() { return valor; }
}
```

## Dois caminhos de erro

| Falha | Mecanismo |
|---|---|
| Input inválido do cliente | `tryCreate` → `Result.fail` |
| Invariante em operação já dentro do domínio | `throw new DomainException(...)` |

O id da entidade raiz não é VO: é o `UUID` de `Entity.getId()`. `CompanyId` continua VO porque a empresa é outro conceito.

## Testes

- `tryCreate` válido devolve valor normalizado.
- `tryCreate` inválido devolve `isFailure()` e a mensagem esperada (`NomeExemplo.OBRIGATORIO` ou `ErroDominio.*.mensagem()`).
- `reconstituir` não valida.
- Cobrir `de`, `zero` e `padrao` quando existirem.

## Armadilhas

- `record` com compact constructor.
- Validação dentro do construtor privado: `reconstituir` passaria a rejeitar dado legado.
- Mensagem no formato de código (`EMPRESA_OBRIGATORIA`) em vez da frase.
- VO com JPA ou CDI.
- Mapper chamando `tryCreate`.
- `new CompanyId(...)`: o construtor é privado. Usar `tryCreate` ou `reconstituir`.
