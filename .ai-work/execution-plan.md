# Plano de execução

Atualizado em 2026-10-29. O plano `docs/features/funil-de-vendas/plan.md` (aprovado em 28/10/2026) entrou como doze tarefas. As tarefas 1 a 7 estão `DONE`. Nesta execução a tarefa 8 está `IN_PROGRESS`. A tarefa 9 permanece `PENDING`.

A worktree `.worktrees/distribuicao-entidades` (branch `feat/distribuicao-entidades`) não é alterada. Nenhuma tarefa do funil disputa arquivo com ela: a tarefa 1 só cria arquivos novos em `tech.techparterns.funil.domain`.

Estados: `PENDING`, `READY`, `IN_PROGRESS`, `BLOCKED`, `REVIEW`, `DONE`, `FAILED`.

## T-CFG-01

| Campo | Valor |
| --- | --- |
| Bounded context | repositório |
| Descrição | Criar agentes, regra de orquestração, índice `specs/` e estado `.ai-work/` sem implementar negócio |
| Especificações | Pedido desta sessão. Fontes lidas: `pom.xml`, `.claude/skills/`, `.cursor/agents/`, `.cursor/commands/feature-workflow.md`, `docs/features/*` |
| Skills | Nenhuma skill de implementação aplicada. Skills existentes foram localizadas, não duplicadas |
| Módulos | Módulo único `techparterns-crm` |
| Dependências | Nenhuma |
| Arquivos | `.cursor/agents/` (novos), `.cursor/rules/orchestration.mdc`, `.cursor/commands/dev-cycle.md`, `specs/**`, `.ai-work/**` |
| Agente | Composer nesta sessão. A tentativa de delegar `domain-analyst` foi rejeitada pelo catálogo |
| Critérios | Agentes novos no disco; agentes antigos preservados; `specs/` sem regra inventada; estado inicial gravado |
| Testes | Checagem estrutural dos arquivos, não `./mvnw test` |
| Estado | `DONE` para os arquivos. A delegação continua bloqueada por B-AGENT |

## T-SPEC-IDENT

| Campo | Valor |
| --- | --- |
| Bounded context | identidade |
| Descrição | Projetar para `specs/contexts/identidade/` só o que `docs/features/identidade-e-acesso/` já diz, mantendo DEL-18 a DEL-21 e MN-O3/MN-O4 como `OPEN` |
| Especificações | `docs/features/identidade-e-acesso/spec.md`, `discovery.md`, `validation.md` |
| Skills | Nenhuma. Análise, não código |
| Módulos | Nenhum arquivo de `src/` |
| Dependências | B-AGENT |
| Arquivos | `specs/contexts/identidade/` |
| Agente | `domain-analyst`, depois `spec-generator` |
| Critérios | Cada regra transcrita cita identificador da fonte. Item aberto não vira confirmado |
| Testes | Não se aplica |
| Estado | `BLOCKED` |

## T-SPEC-LEAD

| Campo | Valor |
| --- | --- |
| Bounded context | lead |
| Descrição | Projetar `docs/features/leads-e-importacao/` para `specs/contexts/lead/` e comparar com a spec de distribuição sem fundir as duas |
| Especificações | `docs/features/leads-e-importacao/` e a fronteira em `docs/features/distribuicao/spec.md` |
| Skills | Nenhuma |
| Módulos | Nenhum arquivo de `src/` |
| Dependências | B-AGENT. Pode seguir em paralelo com T-SPEC-IDENT porque os arquivos de destino são outros |
| Arquivos | `specs/contexts/lead/` |
| Agente | `domain-analyst`, depois `spec-generator` |
| Critérios | Não tratar o trecho antigo da validação (repositório sem lead) como código atual |
| Testes | Não se aplica |
| Estado | `BLOCKED` |

## T-SPEC-DIST

| Campo | Valor |
| --- | --- |
| Bounded context | distribuicao |
| Descrição | Projetar a spec validada e listar ressalvas ainda abertas, sem executar o plano técnico de 2026-10-17 |
| Especificações | `docs/features/distribuicao/` |
| Skills | Nenhuma |
| Módulos | Nenhum arquivo de `src/` |
| Dependências | B-AGENT |
| Arquivos | `specs/contexts/distribuicao/` |
| Agente | `domain-analyst`, depois `spec-generator` |
| Critérios | E-mail, carga de `LeadAtribuido`, intervalo, paginação e critério da RN-DS-19 continuam `OPEN` se a fonte não tiver sido atualizada |
| Testes | Não se aplica |
| Estado | `BLOCKED` |

## T-IMPL-NOVA

| Campo | Valor |
| --- | --- |
| Bounded context | a definir |
| Descrição | Implementar comportamento novo |
| Especificações | Ainda sem tarefa `READY` |
| Skills | As de `.claude/skills/` que o componente exigir |
| Módulos | Módulo único |
| Dependências | Spec utilizável, pontos `OPEN` da tarefa fechados, aprovação humana quando o plano ou a decisão ainda não tiverem autorização de implementar |
| Arquivos | Não definidos |
| Agente | `implementer`, depois `test-engineer`, `code-reviewer` e, se a borda for sensível, `security-reviewer` |
| Critérios | Quality gate da regra de orquestração |
| Testes | `./mvnw test` e, se houver `@QuarkusIntegrationTest` novo, `./mvnw verify -DskipITs=false` |
| Estado | `BLOCKED` |

Não há tarefa de implementação independente desbloqueada fora do funil abaixo. `T-IMPL-NOVA` permanece o guarda-chuva antigo e não é a ordem de implementar o funil.

## Fora do plano do funil

O plano deixa de fora, e nenhuma tarefa implementa:

- Tela Angular e qualquer frontend.
- Edição de `docs/features/distribuicao/spec.md`.
- Observação de perda limitada ou cortada em 300 caracteres.
- Recusa ou arredondamento de uma terceira casa decimal; nenhum `RoundingMode` para esse caso.
- Corpo obrigatório dos eventos `LeadMudouEtapa`, `LeadGanho` e `LeadPerdido`. O teste observa tipo e ocorrência.
- Paginação do quadro e ordem interna da coluna.
- Pedido misto em que o plano de interesse passa de 40 caracteres e outro campo do mesmo pedido é válido.
- Probabilidade numérica lida na `GANHO` nascida e na `PERDIDO` nascida. As duas ficam com probabilidade nula. A `ABERTO` nascida grava 0.
- Nome da etapa nascida como critério. O texto «Aberto», «Ganho» ou «Perdido» só preenche a coluna.
- Default ou recusa nova para probabilidade, cor ou `sla_dias` omitidos na criação. Os testes enviam os valores que o critério cita.
- Reabrir lead. Reativar etapa. Troca de dono. Atividades e agenda. Filtro ou roteiro por cultura, tag ou follow-up. Catálogo de planos do SaaS. Evento `LeadCriado`. Importação como feature própria. Várias colunas ativas de ganho ou de perdido. Flag presa ao nome da etapa.
- Concorrência de dois movimentos do mesmo lead. Bloqueio otimista.
- `@QuarkusIntegrationTest` novo e teste arquitetural automatizado.
- Gatilho da colocação única como regra de produto. A decisão técnica (caso de uso idempotente na subida) fica na tarefa 5, não na tarefa 1.

## T-FV-01

| Campo | Valor |
| --- | --- |
| Bounded context | funil |
| Descrição | Domínio puro: VOs, entidades e serviços de probabilidade, cor, SLA, plano, dinheiro, roteiro, nascimento das três etapas, soma das colunas, atraso e trava de entrada por flag |
| Especificações | `docs/features/funil-de-vendas/plan.md` tarefa 1; `spec.md` RF-FV-08, RF-FV-09, RF-FV-12, RF-FV-15, RF-FV-20, RF-FV-21, RF-FV-22, RF-FV-27, RF-FV-28, RF-FV-29, RF-FV-33, RF-FV-34 e RN correspondentes. Validação APROVADA COM RESSALVAS, sem bloqueio |
| Skills | `entity`, `value-object`, `domain-service` |
| Módulos | Módulo único `techparterns-crm` |
| Dependências | Nenhuma. Não depende de item `OPEN` |
| Arquivos | Novos em `src/main/java/tech/techparterns/funil/domain/model/` e `domain/service/`, testes em `src/test/java/tech/techparterns/funil/domain/`. Referência só de leitura: `Entity`, `Result`, `CompanyId` |
| Agente | `implementer`, depois `test-engineer`, depois `code-reviewer`. Segurança dispensada se a tarefa permanecer só regra pura, sem borda sensível |
| Critérios | Os da tarefa 1 do plano. Testes unitários sem Quarkus |
| Testes | `./mvnw -q compile` exit 0. `./mvnw test` exit 0, 184 testes, 0 falhas. Sem `verify -DskipITs=false` |
| Estado | `DONE` |

## T-FV-02

| Campo | Valor |
| --- | --- |
| Bounded context | funil |
| Descrição | Schema `V6` e repositórios de etapa, motivo, negócio e histórico, mais `ConsultaLeadFunil` |
| Especificações | Plano, tarefa 2 |
| Skills | `repository` |
| Módulos | Módulo único |
| Dependências | T-FV-01 |
| Arquivos | `V6__criar_funil.sql`, repositórios e JPA do funil, `ConsultaLeadFunilImpl`. Sem editar `LeadEntity` nem `FatosLeadImpl` |
| Agente | `implementer`, `test-engineer`, `code-reviewer`, `security-reviewer` (empresa e leitura de lead) |
| Critérios | Os da tarefa 2. Quality gate passou |
| Testes | `./mvnw -q compile` exit 0. `./mvnw test` no `target` sujo saiu 1 na descoberta (0 testes, CDI). `./mvnw clean test` exit 0: 193 testes, 0 falhas, 0 erros, 0 ignorados. Sem `verify -DskipITs=false` |
| Estado | `DONE` |

## T-FV-03

| Campo | Valor |
| --- | --- |
| Bounded context | funil |
| Descrição | Caso de uso que garante as etapas que faltam; cadastro do admin só depois do pedido válido |
| Especificações | Plano, tarefa 3 |
| Skills | `use-case`, `dto` |
| Módulos | Módulo único |
| Dependências | T-FV-02 |
| Arquivos | `GarantirEtapasUseCase`, `CriarEtapaCommand`, teste `@QuarkusTest`. `EtapaRepository.listarPorEmpresa` foi acrescentado porque `buscarPorId` e `salvar` não leem as etapas da empresa; V6 e o restante dos repositórios não mudaram |
| Agente | `implementer`, `test-engineer`, `code-reviewer`, `security-reviewer` |
| Critérios | Os da tarefa 3. Quality gate passou |
| Testes | `./mvnw -q compile` exit 0. `./mvnw test` exit 0: 204 testes, 0 falhas, 0 erros, 0 ignorados. Sem `verify -DskipITs=false` |
| Estado | `DONE` |

## T-FV-04

| Campo | Valor |
| --- | --- |
| Bounded context | funil e lead |
| Descrição | Lead recém-criado entra na `ABERTO` ativa de menor ordem |
| Especificações | Plano, tarefa 4 |
| Skills | `use-case` |
| Módulos | Módulo único |
| Dependências | T-FV-03 |
| Arquivos | `EntrarLeadNoFunil`, `EntrarLeadNoFunilImpl`, edição de `ImportarLeadsUseCase` no ramo que já chama `aoCriar`, `EntradaLeadCriadoTest`, `CapturaEventosFunil` |
| Agente | `implementer`, `test-engineer`, `code-reviewer`, `security-reviewer` |
| Critérios | Os da tarefa 4. Quality gate passou |
| Testes | `./mvnw -q compile` exit 0. `./mvnw test` exit 0: 212 testes, 0 falhas, 0 erros, 0 ignorados. Sem `verify -DskipITs=false` |
| Estado | `DONE` |

## T-FV-05

| Campo | Valor |
| --- | --- |
| Bounded context | funil |
| Descrição | Colocação única dos leads já gravados, na subida, uma transação por lead |
| Especificações | Plano, tarefa 5. Decisão técnica do plano, não regra de produto nova |
| Skills | `use-case` |
| Módulos | Módulo único |
| Dependências | T-FV-03 |
| Arquivos | `ColocarLeadsJaGravadosUseCase`, `ColocacaoFunilNaSubida`. `SemeadorDono` só como padrão, sem edição |
| Agente | `implementer`, `test-engineer`, `code-reviewer`, `security-reviewer` |
| Critérios | Os da tarefa 5. Quality gate passou |
| Testes | `./mvnw -q compile` exit 0. `./mvnw test` exit 0: 221 testes, 0 falhas, 0 erros, 0 ignorados. Sem `verify -DskipITs=false` |
| Estado | `DONE` |

## T-FV-06

| Campo | Valor |
| --- | --- |
| Bounded context | funil |
| Descrição | Admin cadastra, ativa e desativa motivo de perda da própria empresa |
| Especificações | Plano, tarefa 6 |
| Skills | `use-case`, `dto`, `resource` |
| Módulos | Módulo único |
| Dependências | T-FV-02 |
| Arquivos | `CadastrarMotivoPerdaUseCase`, `AlterarAtivoMotivoPerdaUseCase`, `ListarMotivosPerdaUseCase`, commands, query, DTOs, `MotivoPerdaResource`, `MotivoPerdaResourceTest`. `MotivoPerdaRepository` ganhou `existeNome` e `listarPorEmpresa`. V6 e `MotivoPerda` sem edição |
| Agente | `implementer`, `test-engineer`, `code-reviewer`, `security-reviewer` |
| Critérios | Os da tarefa 6. Quality gate passou |
| Testes | `./mvnw -q compile` exit 0. `./mvnw test` exit 0: 228 testes, 0 falhas, 0 erros, 0 ignorados. Sem `verify -DskipITs=false` |
| Estado | `DONE` |

## T-FV-07

| Campo | Valor |
| --- | --- |
| Bounded context | funil |
| Descrição | Administração de etapas restrita ao admin |
| Especificações | Plano, tarefa 7 |
| Skills | `use-case`, `dto`, `resource` |
| Módulos | Módulo único |
| Dependências | T-FV-03 |
| Arquivos | `CriarEtapaUseCase`, `AlterarEtapaUseCase`, `DesativarEtapaUseCase`, `ApagarEtapaUseCase`, commands, `EtapaCriadaDto`, `EtapaFunilResource`, `EtapaFunilResourceTest`. Fora da lista, com motivo: `Etapa.alterar` e `desativar`; `NegocioFunilRepository.existeNaEtapa`. Sem reativar etapa. V6 e `GarantirEtapasUseCase` sem edição de lógica |
| Agente | `implementer`, `test-engineer`, `code-reviewer`, `security-reviewer` |
| Critérios | Os da tarefa 7. Quality gate passou |
| Testes | `./mvnw -q compile` exit 0. `./mvnw test` exit 0: 243 testes, 0 falhas, 0 erros, 0 ignorados. Sem `verify -DskipITs=false` |
| Estado | `DONE` |

## T-FV-08

| Campo | Valor |
| --- | --- |
| Bounded context | funil |
| Descrição | Roteiro, demo e dados comerciais do lead aberto, sem mudar a etapa |
| Especificações | Plano, tarefa 8 |
| Skills | `use-case`, `dto`, `resource` |
| Módulos | Módulo único |
| Dependências | T-FV-02 |
| Arquivos | `AtualizarDadosAbertosUseCase` e `PATCH /funil/leads/{leadId}` |
| Agente | `implementer`, `test-engineer`, `code-reviewer`, `security-reviewer` |
| Critérios | Os da tarefa 8 |
| Testes | `./mvnw test` |
| Estado | `IN_PROGRESS` |

## T-FV-09

| Campo | Valor |
| --- | --- |
| Bounded context | funil |
| Descrição | Movimento entre etapas `ABERTO` ativas, histórico e `LeadMudouEtapa` |
| Especificações | Plano, tarefa 9 |
| Skills | `use-case`, `dto`, `resource` |
| Módulos | Módulo único |
| Dependências | T-FV-07 e T-FV-08 |
| Arquivos | `MoverLeadEtapaUseCase` e `POST /funil/leads/{leadId}/mover`. `ExcluirLeadUseCase` sem edição de comportamento |
| Agente | `implementer`, `test-engineer`, `code-reviewer`, `security-reviewer` |
| Critérios | Os da tarefa 9 |
| Testes | `./mvnw test` |
| Estado | `PENDING` |

## T-FV-10

| Campo | Valor |
| --- | --- |
| Bounded context | funil e lead |
| Descrição | Ganho e perdido pelo funil, situação no lead e evento final |
| Especificações | Plano, tarefa 10 |
| Skills | `use-case`, `dto`, `resource` |
| Módulos | Módulo único |
| Dependências | T-FV-06 e T-FV-09 |
| Arquivos | `AtualizarFatosLead`, `AtualizarFatosLeadImpl`, `MarcarGanhoUseCase`, `MarcarPerdidoUseCase` e as rotas de ganho e perdido. Sem editar consultas de carga, fila, rodízio e redistribuição |
| Agente | `implementer`, `test-engineer`, `code-reviewer`, `security-reviewer` |
| Critérios | Os da tarefa 10 |
| Testes | `./mvnw test` |
| Estado | `PENDING` |

## T-FV-11

| Campo | Valor |
| --- | --- |
| Bounded context | funil e distribuicao |
| Descrição | `PATCH /leads/{id}` cumpre a RN-FV-04. Pedido `ABERTA` permanece no ramo atual |
| Especificações | Plano, tarefa 11. `docs/features/distribuicao/spec.md` não é editado |
| Skills | `use-case`, `dto` |
| Módulos | Módulo único |
| Dependências | T-FV-10 |
| Arquivos | `Contratos.Fatos`, `EditarFatosLead`, `LeadResource` só se o JSON precisar dos campos, `FecharLeadNoFunil` e a implementação |
| Agente | `implementer`, `test-engineer`, `code-reviewer`, `security-reviewer` |
| Critérios | Os da tarefa 11 |
| Testes | `./mvnw test` |
| Estado | `PENDING` |

## T-FV-12

| Campo | Valor |
| --- | --- |
| Bounded context | funil |
| Descrição | Quadro por empresa e por ator, sem página e sem dados extras de card |
| Especificações | Plano, tarefa 12 |
| Skills | `query-cqrs`, `dto`, `resource` |
| Módulos | Módulo único |
| Dependências | T-FV-05, T-FV-07 e T-FV-10 |
| Arquivos | `QuadroFunilQuery` (só `uf` e `quente`), caso de uso, DTO e `GET /funil/quadro` |
| Agente | `implementer`, `test-engineer`, `code-reviewer`, `security-reviewer` |
| Critérios | Os da tarefa 12 |
| Testes | `./mvnw test` |
| Estado | `PENDING` |
