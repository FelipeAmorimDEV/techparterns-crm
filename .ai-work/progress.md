# Progresso

## Checkpoint

- Data: 2026-10-29
- Sessão: dev-cycle do Funil de Vendas, tarefa 6, no branch `develop`
- Última tarefa concluída: `T-FV-07` (`DONE`). Quality gate passou
- Tarefa atual: `T-FV-08` (`IN_PROGRESS`)
- Próximo passo: concluir o ciclo da `T-FV-08` até o quality gate. `T-FV-09` permanece `PENDING` (depende de `T-FV-07` e `T-FV-08`)
- Worktree `.worktrees/distribuicao-entidades` não foi modificada

### T-FV-01

- Implementação: subagente `implementer` (id `02009fdd-8dfb-4bda-8ba3-a958bf9aeaf9`). Skills `entity`, `value-object` e `domain-service` lidas por ele. Só arquivos novos em `tech.techparterns.funil.domain`.
- Testes: subagente `test-engineer` (id `bd983d09-43cc-40e1-a94c-abe7c2f74d31`). A primeira `./mvnw test` dele saiu 1 porque o cliente Docker padrão não respondia e o Surefire abortou com 0 testes. Nesta sessão, em seguida, `./mvnw test` achou o socket do Docker Desktop e terminou exit 0: 184 testes, 0 falhas. `./mvnw -q compile` exit 0.
- Revisão de código: subagente `code-reviewer` (id `38d8e80e-89bb-424f-9515-bf8e6ca2892b`), somente leitura, decisão **APROVADA**. Nenhum ciclo de correção.
- Segurança: dispensada pelo orquestrador. A tarefa é regra pura, sem autenticação, autorização, borda de tenant, dado pessoal, segredo, upload ou integração. O revisor de segurança não foi chamado.
- O ciclo do comando colocou a implementação antes dos testes. Não houve uma rodada vermelha separada antes do código de produção.
- Sem commit. `dev-keys` não foi lido.

### T-FV-02

- Implementação: subagente `implementer` (id `d3b5c563-379f-4a31-929e-9cad889230dd`). Skill `repository` e `references/repository-pattern.md`. Schema `V6`, quatro repositórios, porta `ConsultaLeadFunil` e `ConsultaLeadFunilImpl`. Domínio da tarefa 1, `LeadEntity` e `FatosLeadImpl` sem edição.
- Testes: subagente `test-engineer` (id `84c12b2f-645b-4312-9c4c-7871163a52f4`), arquivo `FunilRepositoryTest` (9 testes). Nesta sessão, em seguida, `./mvnw test` no `target` já compilado saiu 1: descoberta do Quarkus com 0 testes e dependência `LeadRepository` não satisfeita. Hipótese: augmentação velha. `./mvnw clean test` saiu 0: 193 testes, 0 falhas, 0 erros, 0 ignorados. Sem ciclo de correção de código.
- Revisão de código: subagente `code-reviewer` (id `d718c646-0e0b-4e16-8cbd-a117bd10b805`), somente leitura, decisão **APROVADA**.
- Segurança: subagente `security-reviewer` (id `16a3e7fa-6cfd-4186-a01e-bb3f042b692b`), somente leitura, decisão **SEM BLOQUEIO**. A tarefa filtra empresa e lê situação, UF, cidade, quente, exclusão e dono. Não há resource novo.
- O ciclo colocou a implementação antes dos testes. Não houve rodada vermelha separada antes do código de produção.
- Sem commit. `dev-keys` não foi lido. Worktree `.worktrees/distribuicao-entidades` não foi modificada.

### T-FV-03

- Implementação: subagente `implementer` (id `253a61c7-e664-4345-8d83-c332bcedb21f`). Skills `use-case` e `dto`, com os `references` de padrão. `GarantirEtapasUseCase` (`execute` com empresa do token; `garantirAusentes` sem token) e `CriarEtapaCommand`. `EtapaRepository` ganhou só `listarPorEmpresa`, porque a interface da tarefa 2 não lia as etapas da empresa. V6, domínio da tarefa 1 e os outros repositórios sem edição.
- Testes: subagente `test-engineer` (id `2f1ffe6e-194e-4d7e-9145-0dbacc7d1954`), arquivo `GarantirEtapasUseCaseTest` (11 testes). Nesta sessão, em seguida, `./mvnw -q compile` exit 0 e `./mvnw test` exit 0: 204 testes, 0 falhas, 0 erros, 0 ignorados. Sem `./mvnw clean test`. Sem `verify -DskipITs=false`.
- Revisão de código: subagente `code-reviewer` (id `f8bf5f51-05ba-4c6b-8bd6-b4c7b75b0c9f`), somente leitura, decisão **APROVADA**. Nenhum ciclo de correção.
- Segurança: subagente `security-reviewer` (id `812e16bf-6282-4a4c-8260-2fa8692ccc11`), somente leitura, decisão **SEM BLOQUEIO**. A empresa do `execute` sai do token; `listarPorEmpresa` filtra `company_id`. O papel `ADMIN` na rota fica para a tarefa 7, que ainda não tem resource. Achado não bloqueante registrado em `decisions.md`.
- O ciclo colocou a implementação antes dos testes. Não houve rodada vermelha separada antes do código de produção.
- Sem commit. `dev-keys` não foi lido. Worktree `.worktrees/distribuicao-entidades` não foi modificada.

### T-FV-04

- Implementação: subagente `implementer` (id `419376f1-514b-4943-9c1f-35d775fda899`). Skills `use-case` e `references/use-case-pattern.md`. Porta `EntrarLeadNoFunil`, `EntrarLeadNoFunilImpl` e chamada em `ImportarLeadsUseCase` depois de `aoCriar`. Domínio da tarefa 1, V6, repositórios, `GarantirEtapasUseCase`, `LeadEntity` e `FatosLeadImpl` sem edição.
- Testes: subagente `test-engineer` (id `7520d136-e605-4acf-818e-15c98046d6cb`), `EntradaLeadCriadoTest` (8 testes) e `CapturaEventosFunil`. Nesta sessão, em seguida, `./mvnw -q compile` exit 0 e `./mvnw test` exit 0: 212 testes, 0 falhas, 0 erros, 0 ignorados. Sem `./mvnw clean test`. Sem `verify -DskipITs=false`.
- Revisão de código: subagente `code-reviewer` (id `8717b74f-a1cf-4999-a1bd-5094206b6b1e`), somente leitura, decisão **APROVADA**. Nenhum ciclo de correção.
- Segurança: subagente `security-reviewer` (id `2e015375-9149-4ec3-bdc3-55e6843ff51e`), somente leitura, decisão **SEM BLOQUEIO**. A empresa do lead nasce do token no `ImportarLeadsUseCase`; a impl usa `lead.companyId()`. Achado não bloqueante registrado em `decisions.md`.
- O ciclo colocou a implementação antes dos testes. Não houve rodada vermelha separada antes do código de produção.
- Sem commit. `dev-keys` não foi lido. Worktree `.worktrees/distribuicao-entidades` não foi modificada.

### T-FV-05

- Implementação: subagente `implementer` (id `c09b0e37-972a-4c6b-84f3-c0422dcd5491`). Skills `use-case` e `references/use-case-pattern.md`. `ColocarLeadsJaGravadosUseCase` e `ColocacaoFunilNaSubida`. Fora da lista do plano, com motivo: `ColocarLeadsJaGravadosCommand` (record vazio; `UseCase` exige input), porta `LeadsSemLinhaFunil` e `LeadsSemLinhaFunilImpl`. A empresa sai da linha do lead. `EntrarLeadNoFunilImpl`, `ImportarLeadsUseCase` e `SemeadorDono` sem edição.
- Testes: subagente `test-engineer` (id `7b92e678-a145-45eb-b4f4-4f313709555c`), `ColocarLeadsJaGravadosUseCaseTest` (9 testes). A primeira `./mvnw test` dele saiu 1: o `UPDATE` para situação `FORA` bateu no `CHECK` de `V5`. O teste passou a suspender e restaurar esse check só nessa prova. `src/main/` não foi alterado nessa correção de teste.
- Nesta sessão, em seguida, `./mvnw -q compile` exit 0 e `./mvnw test` exit 0: 221 testes, 0 falhas, 0 erros, 0 ignorados. Sem `./mvnw clean test`. Sem `verify -DskipITs=false`.
- Revisão de código: subagente `code-reviewer` (id `7531f111-ea5b-473e-b263-d203e18f4d17`), somente leitura, decisão **APROVADA**. O teste que suspende o `CHECK` ficou como risco, não defeito. Nenhum ciclo de correção de produção.
- Segurança: subagente `security-reviewer` (id `965d06fe-fdab-4c13-8b45-8cf0d0884943`), somente leitura, decisão **SEM BLOQUEIO**. Achado não bloqueante registrado em `decisions.md`.
- O ciclo colocou a implementação antes dos testes. Não houve rodada vermelha separada antes do código de produção.
- Sem commit. `dev-keys` não foi lido. Worktree `.worktrees/distribuicao-entidades` não foi modificada.

### T-FV-06

- Implementação: subagente `implementer` (id `473cb694-bc85-44df-b098-014e79054291`). Skills `use-case`, `dto` e `resource`, com os `references` de padrão. `CadastrarMotivoPerdaUseCase`, `AlterarAtivoMotivoPerdaUseCase`, `MotivoPerdaResource`. Fora da lista nominal do plano, com motivo: `ListarMotivosPerdaUseCase`, a query e os DTOs, porque o critério exige listagem filtrada pela empresa do token. `MotivoPerdaRepository` ganhou `existeNome` e `listarPorEmpresa`. V6, `MotivoPerda` e as tarefas 1 a 5 sem edição.
- Testes: subagente `test-engineer` (id `32c784ea-d288-43ec-bd4f-99dbe95b5dd8`), `MotivoPerdaResourceTest` (7 testes). Nesta sessão, em seguida, `./mvnw -q compile` exit 0 e `./mvnw test` exit 0: 228 testes, 0 falhas, 0 erros, 0 ignorados. Sem `./mvnw clean test`. Sem `verify -DskipITs=false`.
- Revisão de código: subagente `code-reviewer` (id `3d900b47-2d40-468b-aced-19458fefccab`), somente leitura, decisão **APROVADA**. Nenhum ciclo de correção.
- Segurança: subagente `security-reviewer` (id `fdd44dc0-10e0-43d9-840f-521b54a473ec`), somente leitura, decisão **SEM BLOQUEIO**. Achados não bloqueantes registrados em `decisions.md`.
- O ciclo colocou a implementação antes dos testes. A skill de TDD pede o teste vermelho antes do código; esta execução seguiu a ordem do comando, como nas tarefas 1 a 5. Não houve rodada vermelha separada.
- Sem commit. `dev-keys` não foi lido. Worktree `.worktrees/distribuicao-entidades` não foi modificada.

### T-FV-07

- Implementação: subagente `implementer` (id `f4218619-b8cf-4cf8-b075-84b66fad6e68`). Skills `use-case`, `dto`, `resource`, `entity` e `repository`, com os `references` de padrão. `CriarEtapaUseCase` delega a `GarantirEtapasUseCase`. `AlterarEtapaUseCase`, `DesativarEtapaUseCase`, `ApagarEtapaUseCase` e `EtapaFunilResource` (`POST`, `PATCH`, `POST .../desativar`, `DELETE`), todas com `@RolesAllowed("ADMIN")`. Fora da lista do plano, com motivo: `Etapa.alterar` e `desativar`, porque os campos eram `final`; `NegocioFunilRepository.existeNaEtapa`, porque nenhum método dizia se a etapa tem lead. V6, motivos e a lógica de `GarantirEtapasUseCase` sem edição. Sem reativar etapa.
- Testes: subagente `test-engineer` (id `dd307c99-f77d-4fa4-a7f4-48b473767787`), `EtapaFunilResourceTest` (15 testes). A primeira chamada de desativar saiu 415 porque o cliente mandou `application/x-www-form-urlencoded`. O teste passou a enviar `application/json` sem corpo. `src/main/` não foi alterado nessa correção de teste.
- Nesta sessão, em seguida, `./mvnw -q compile` exit 0 e `./mvnw test` exit 0: 243 testes, 0 falhas, 0 erros, 0 ignorados. Sem `./mvnw clean test`. Sem `verify -DskipITs=false`.
- Revisão de código: subagente `code-reviewer` (id `73b0e807-36f6-4015-94ce-8b9ac496fe3d`), somente leitura, decisão **APROVADA**. Nenhum ciclo de correção.
- Segurança: subagente `security-reviewer` (id `21d76846-63de-4894-ba98-abc741af9acf`), somente leitura, decisão **SEM BLOQUEIO**. Achados não bloqueantes registrados em `decisions.md`.
- O ciclo colocou a implementação antes dos testes. A skill de TDD pede o teste vermelho antes do código; esta execução seguiu a ordem do comando, como nas tarefas 1 a 6. Não houve rodada vermelha separada.
- Sem commit. `dev-keys` não foi lido. Worktree `.worktrees/distribuicao-entidades` não foi modificada.

## Git

Worktree principal: `/home/felipeamorim/Desktop/techparterns-crm`, branch `develop`.

Outra worktree já existente: `.worktrees/distribuicao-entidades`, branch `feat/distribuicao-entidades`. Não foi modificada nesta configuração.

## Verificações

- Checagem estrutural em Python: frontmatter dos 11 agentes, `alwaysApply` da regra, 25 arquivos em `specs/` e os quatro arquivos de `.ai-work/`. Resultado: `OK`.
- `git diff` dos quatro agentes antigos e de `feature-workflow.md`: sem alteração.
- `./mvnw` não foi executado. A configuração não altera `src/`.
- Tentativa de delegar `domain-analyst` pela ferramenta `Task`: rejeitada. O catálogo aceito nesta sessão não inclui os sete agentes novos.

## Falhas conhecidas

- Em 2026-10-24 a `Task` não aceitava os agentes novos. Nesta sessão aceitou `implementer`, `test-engineer`, `code-reviewer` e `security-reviewer`. As tarefas `T-SPEC-*` seguem paradas. Ver B-AGENT.
- `docs/` está no `.gitignore`. As specs de feature existem no disco e não entram no commit por essa regra.
- Conformidade entre o código atual e as specs de outubro de 2026 não foi auditada.
