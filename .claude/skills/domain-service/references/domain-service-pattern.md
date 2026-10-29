# Domain Service Pattern (techparterns-crm — Quarkus)

Pacote: `tech.techparterns.<modulo>.domain.service`.

## Quando criar

- A regra usa mais de um objeto e não pertence a um só.
- Dois use cases precisam da mesma política.
- O cálculo é determinístico.

Não criar para orquestrar repositório, transação ou HTTP. Isso é use case.

## Estático

```java
package tech.techparterns.<modulo>.domain.service;

import tech.techparterns.<modulo>.domain.model.ExemploValor;

public final class ExemploCalculator {

    private ExemploCalculator() {}

    public static ExemploValor somar(ExemploValor a, ExemploValor b) {
        return ExemploValor.de(a.valor().add(b.valor()));
    }
}
```

## Instância imutável

```java
package tech.techparterns.<modulo>.domain.service;

import tech.techparterns.<modulo>.domain.model.ExemploValor;

public final class LimiteValorPolicy {

    private final ExemploValor limite;

    public LimiteValorPolicy(ExemploValor limite) {
        this.limite = limite;
    }

    public boolean acimaDoLimite(ExemploValor atual) {
        return atual.valor().compareTo(limite.valor()) > 0;
    }
}
```

Sem `@ApplicationScoped`. O use case instancia ou recebe a política pronta.

## Checklist

- Arquivo sob `domain/`.
- Sem CDI, sem JPA, sem I/O.
- Construtor privado na classe só de estáticos.
- Teste do limite, do igual e do zero quando o tipo for numérico.

## Armadilhas

- Serviço para uma regra que seria `exemplo.renomear(...)`.
- Serviço recebendo repositório ou `TenantContext`.
- Duplicar validação que o VO já faz em `tryCreate`.
- `@ApplicationScoped` "para poder injetar".
