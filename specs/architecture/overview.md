# Visão da arquitetura

Fatos lidos do repositório em 2026-10-24. Não são decisões de negócio novas.

| Fato | Onde | Classificação |
| --- | --- | --- |
| Um projeto Maven, artifact `techparterns-crm`, packaging `quarkus`, versão `1.0.0-SNAPSHOT` | `pom.xml` | `CONFIRMED` como fato de build |
| Java `maven.compiler.release` 21 | `pom.xml` | `CONFIRMED` como fato de build |
| Quarkus BOM `3.40.1` | `pom.xml` | `CONFIRMED` como fato de build |
| Não há `<modules>` | `pom.xml` | `CONFIRMED` como fato de build |
| Wrapper Maven presente | `mvnw` | `CONFIRMED` |
| PostgreSQL em dev/prod; teste com Dev Services `postgres:16-alpine` | `src/main/resources/application.properties` | `CONFIRMED` como configuração |
| Flyway `migrate-at-start` | mesmo arquivo | `CONFIRMED` como configuração |
| Migrações `V1` a `V5` | `src/main/resources/db/migration/` | `CONFIRMED` como arquivos existentes |
| JWT SmallRye, REST, Hibernate ORM, mailer | `pom.xml` | `CONFIRMED` como dependências |
| `skipITs` padrão `true` | `pom.xml` | `CONFIRMED` |
| Sem ArchUnit | busca em `pom.xml` e testes | `CONFIRMED` como ausência |
| Frontend Angular não está neste repositório | inspeção da árvore | `CONFIRMED` como ausência |

A frase "Maven multi-módulo" não descreve este repositório. Os limites atuais são pacotes dentro do monólito.

Plano técnico não aprovado como mudança desta sessão: `docs/superpowers/plans/2026-10-17-distribuicao-no-padrao.md`. Há worktree Git em `.worktrees/distribuicao-entidades`, branch `feat/distribuicao-entidades`. Essa branch não foi alterada aqui.
