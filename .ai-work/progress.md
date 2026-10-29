# Progresso

## Checkpoint

- Data: 2026-10-29
- Sessão: tarefa 10 do Funil de Vendas, no branch `feat/funil-de-vendas-eaf0`
- Última tarefa concluída: `T-FV-10` (`DONE`). Quality gate passou. Revisão de código **APROVADA**. Revisão de segurança **SEM BLOQUEIO**
- Tarefa atual: nenhuma em andamento
- Próximo passo: `T-FV-11` está `READY`. `T-FV-12` também está `READY` e não disputa arquivo com a 11
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

### T-FV-08

- Implementação: subagente `implementer` (id `c57afaa7-315b-4653-95bb-1531df5e203b`). Skills `use-case`, `dto`, `resource` e `entity`, com os `references` de padrão. `AtualizarDadosAbertosUseCase`, `AtualizarDadosAbertosCommand`, `DadosAbertosDto` e `NegocioFunilResource` (`PATCH /funil/leads/{leadId}`, papéis `ADMIN` e `SELLER`). Fora da lista do plano, com motivo: `NegocioFunil.comDadosAbertos`, porque os campos comerciais eram `final`. A etapa, a entrada, o plano fechado, o valor fechado, o motivo e a observação permanecem.
- Durante o ciclo, arquivos não rastreados do funil sumiram do disco. Foram recolocados a partir do histórico local e das classes já compiladas. `dev-keys` não foi lido nem recriado. A worktree `.worktrees/distribuicao-entidades` não foi modificada.
- Testes: subagente `test-engineer` (id `fa2c0fb0-37c8-4aef-9221-0e62c00a8838`), `AtualizarDadosAbertosUseCaseTest` (10 testes). A primeira execução dele saiu 1: seis testes do vendedor receberam HTTP 500 porque o subject do JWT vinha vazio. O teste passou a enviar o claim `sub`. Nesta sessão, em seguida, `./mvnw -q compile` exit 0 e `./mvnw test` exit 0: 253 testes, 0 falhas, 0 erros, 0 ignorados. O Docker Desktop precisou ser iniciado; `TESTCONTAINERS_RYUK_DISABLED=true` evitou a montagem do socket que o Desktop recusa. Sem `verify -DskipITs=false`.
- O teste `falhaDeUmLeadNaoApagaLinhaDoOutro` da tarefa 5, recuperado sem o ajuste já aprovado, voltou a suspender o `CHECK` de situação só nessa prova (`DROP CONSTRAINT IF EXISTS` e recriação `NOT VALID`). `src/main/` não mudou nessa correção.
- Revisão de código: subagente `code-reviewer` (id `7de9c1c6-0e20-4ca0-a873-2929d02c7827`), somente leitura, decisão **APROVADA**. Nenhum ciclo de correção de produção.
- Segurança: subagente `security-reviewer` (id `4956cf8b-1b43-4ad3-9b54-6b8d0e91eb39`), somente leitura, decisão **SEM BLOQUEIO**. Achado não bloqueante registrado em `decisions.md`.
- O ciclo colocou a implementação antes dos testes. Não houve rodada vermelha separada antes do código de produção.
- Sem commit.

### T-FV-09

- Implementação: subagente `implementer` (id `9e21f103-2e58-44b6-bcac-21f4197b0bdc`). Skills `use-case`, `dto` e `resource`, com os `references` de padrão. A skill `entity` não foi lida porque `NegocioFunil` não foi alterado nesta passagem. O caso de uso, o command, `NegocioFunil.moverPara` e o `POST /funil/leads/{leadId}/mover` já estavam no disco; o ajuste foi recusar `etapaId` nulo antes de consultar a etapa. `ExcluirLeadUseCase` não foi editado.
- Testes: subagente `test-engineer` (id `aec0a8ef-c67b-4735-9ba7-12a414e459a4`), `MoverLeadEtapaUseCaseTest` (12 testes). `./mvnw -q compile` exit 0, registrado pelo implementer. `./mvnw test` exit 0: 265 testes, 0 falhas, 0 erros, 0 ignorados. A soma dos relatórios Surefire confirma 265. Sem `./mvnw clean test`. Sem `TESTCONTAINERS_RYUK_DISABLED`. Sem `verify -DskipITs=false`.
- Revisão de código: subagente `code-reviewer` (id `88300750-3439-44b4-a81e-2f805b7a4814`), somente leitura, decisão **APROVADA**. Nenhum ciclo de correção.
- Segurança: subagente `security-reviewer` (id `fd1c354f-a5d4-4233-930a-b05af3c66013`), somente leitura, decisão **SEM BLOQUEIO**.
- O ciclo colocou a implementação antes dos testes. Não houve rodada vermelha separada antes do código de produção.
- Sem commit. `dev-keys` não foi lido. Worktree `.worktrees/distribuicao-entidades` não foi modificada.
- Nesta sessão, no branch `feat/funil-de-vendas-eaf0`, o `test-engineer` (id `bc-ca49ea92-03ce-55ef-88fc-d8103026328c`) reforçou as asserções de histórico e evento em `MoverLeadEtapaUseCaseTest` e rodou `TESTCONTAINERS_RYUK_DISABLED=true DOCKER_HOST=unix:///home/felipeamorim/.docker/desktop/docker.sock ./mvnw test`. Exit 0. Soma dos relatórios Surefire: 44 classes, 265 testes, 0 falhas, 0 erros, 0 ignorados. Revisão de código (id `bc-09b752f1-daad-559d-b74a-11a567264b3b`) **APROVADA**. Revisão de segurança (id `bc-33b677cd-f263-5d0a-9303-abdc05f0dfd5`) **SEM BLOQUEIO**. Achados não bloqueantes em `decisions.md`.

### T-FV-10

- Implementação: subagente `implementer` (id `bc-bf745869-018c-585f-9ffb-79c12f5d0426`). Skills `use-case`, `dto`, `resource` e `entity`, com os `references` de padrão. Porta `AtualizarFatosLead`, `AtualizarFatosLeadImpl`, `MarcarGanhoUseCase`, `MarcarPerdidoUseCase`, commands e `POST /funil/leads/{leadId}/ganho` e `/perdido`. Fora da lista, com motivo: `NegocioFunil.comGanho` e `comPerdido`, porque os campos são `final`. Consultas de carga, fila, rodízio e redistribuição sem edição. `EditarFatosLead` sem edição. `./mvnw -q compile` exit 0.
- Testes: subagente `test-engineer` (id `bc-1487bf88-22af-5194-8cba-af27449422c1`), `MarcarFinalUseCaseTest`. Comando: `TESTCONTAINERS_RYUK_DISABLED=true DOCKER_HOST=unix:///home/felipeamorim/.docker/desktop/docker.sock ./mvnw test`. O relatório Surefire no disco, posterior ao arquivo de teste, registra 10 testes na classe e 275 na suíte, 0 falhas, 0 erros, 0 ignorados, 45 classes. O décimo teste observa o rollback quando a escrita do ganho falha depois da primeira gravação. Sem `verify -DskipITs=false`.
- Revisão de código: subagente `code-reviewer` (id `bc-db770040-6c19-5a4b-a082-2b96074b9f72`), somente leitura, decisão **APROVADA**. Nenhum ciclo de correção.
- Segurança: subagente `security-reviewer` (id `bc-5f8e1de2-249b-5f85-ae7c-63d1ec04fc70`), somente leitura, decisão **SEM BLOQUEIO**. Achado não bloqueante em `decisions.md`.
- O ciclo colocou a implementação antes dos testes. Não houve rodada vermelha separada antes do código de produção.
- `dev-keys` não foi lido. Worktree `.worktrees/distribuicao-entidades` não foi modificada.

## Git

Worktree principal: `/home/felipeamorim/Desktop/techparterns-crm`, branch `feat/funil-de-vendas-eaf0`.

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
