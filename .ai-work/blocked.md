# Bloqueios

## B-AGENT

- Causa: em 2026-10-24 a ferramenta `Task` rejeitou `subagent_type` `domain-analyst`. Os tipos aceitos nessa chamada foram os quatro agentes de feature já existentes (`feature-brainstormer`, `spec-writer`, `spec-validator`, `implementation-planner`) e os subagentes gerais da instalação. Os sete arquivos novos estão em `.cursor/agents/`, fora desse catálogo.
- Dependências: `T-SPEC-IDENT`, `T-SPEC-LEAD`, `T-SPEC-DIST` e qualquer delegação a `domain-analyst`, `spec-generator`, `architect`, `implementer`, `test-engineer`, `code-reviewer` ou `security-reviewer`.
- Ação: recarregar os agentes do Cursor e confirmar que esses `subagent_type` aparecem. Sem isso, não afirmar que foram executados.
- Atualização em 2026-10-28: o catálogo desta sessão aceitou `implementer`, `test-engineer`, `code-reviewer` e `security-reviewer`. A `T-FV-01` e a `T-FV-02` foram delegadas e concluídas. As tarefas `T-SPEC-*` continuam paradas porque esta execução não as retoma.
- Tarefas independentes que seguem: o funil, que não depende dos itens abertos de identidade nem de distribuição.

Não há bloqueio aberto da `T-FV-01`, da `T-FV-02`, da `T-FV-03`, da `T-FV-04`, da `T-FV-05`, da `T-FV-06` nem da `T-FV-07`. A revisão de segurança da tarefa 3 deixou um achado não bloqueante: a checagem de papel `ADMIN` fica na tarefa 7, antes de chamar `execute`. A tarefa 7 colocou `@RolesAllowed("ADMIN")` nas quatro rotas de etapa. A revisão de código da tarefa 7 deixou como risco, não bloqueio, o apagar de etapa vazia mesmo sendo a única do tipo, e a ordem que continua ocupada depois do `deleted_at`. A revisão de segurança da tarefa 7 deixou outro, também não bloqueante: o teste de outra empresa não chama o desativar. A da tarefa 4 deixou outro, também não bloqueante: `salvar` de negócio e de histórico não reconferem a empresa; o único chamador desta tarefa já usa o lead do token. A da tarefa 5 deixou outro, também não bloqueante: o log de `Result` com falha na subida não roda hoje, porque `execute` devolve sucesso. A da tarefa 6 deixou dois, também não bloqueantes: o papel fica na rota, e não há teste de `PATCH` com id de outra empresa. A primeira `./mvnw test` da tarefa 2 falhou com o Docker padrão inativo; a repetição naquela sessão usou o socket do Docker Desktop e passou. Isso não permanece como bloqueio. O teste de falha isolada da tarefa 5 suspende e restaura o `CHECK` de situação; o revisor de código tratou isso como risco, não como bloqueio.

## B-SPEC-ABERTA

- Causa: identidade tem DEL-18, DEL-19, DEL-20, DEL-21, MN-O3 e MN-O4 em aberto no `validation.md`. Distribuição tem texto de e-mail, carga de `LeadAtribuido`, intervalo, paginação e critério da RN-DS-19 em aberto. Não há aprovação localizada do plano concluído como ordem de implementar.
- Dependências: `T-IMPL-NOVA` e qualquer tarefa cujo comportamento dependa desses itens.
- Ação: decisão humana em cada item. Não preencher por inferência.
- Tarefas que não dependem disso: projeção documental das três specs, quando B-AGENT cair.

## B-CONFORMIDADE

- Causa: os planos dizem que a feature não estava implementada, e o código atual já tem os três pacotes. Ninguém nesta sessão comparou código e spec.
- Dependências: declarar qualquer feature como `DONE`.
- Ação: auditoria explícita, com testes executados, fora desta configuração.
- Não bloqueia a escrita do índice em `specs/`.

## B-GITIGNORE-DOCS

- Causa: `.gitignore` ignora `docs/`. As specs de negócio ficam só no disco local.
- Dependências: levar `discovery.md`, `spec.md`, `validation.md` e `plan.md` para o remoto.
- Ação: autorização para deixar de ignorar `docs/features/` ou para copiar a norma para um caminho versionado, sem divergir.
- Enquanto isso, agentes precisam abrir `docs/features/` pelo caminho, porque a busca pode omitir a pasta.
