# Bloqueios

## B-AGENT

- Causa: em 2026-10-24 a ferramenta `Task` rejeitou `subagent_type` `domain-analyst`. Os tipos aceitos nessa chamada foram os quatro agentes de feature já existentes (`feature-brainstormer`, `spec-writer`, `spec-validator`, `implementation-planner`) e os subagentes gerais da instalação. Os sete arquivos novos estão em `.cursor/agents/`, fora desse catálogo.
- Dependências: `T-SPEC-IDENT`, `T-SPEC-LEAD`, `T-SPEC-DIST` e qualquer delegação a `domain-analyst`, `spec-generator`, `architect`, `implementer`, `test-engineer`, `code-reviewer` ou `security-reviewer`.
- Ação: recarregar os agentes do Cursor e confirmar que esses `subagent_type` aparecem. Sem isso, não afirmar que foram executados.
- Atualização em 2026-10-28: o catálogo desta sessão aceitou `implementer`, `test-engineer`, `code-reviewer` e `security-reviewer`. A `T-FV-01` e a `T-FV-02` foram delegadas e concluídas. As tarefas `T-SPEC-*` continuam paradas porque esta execução não as retoma.
- Tarefas independentes que seguem: o funil, que não depende dos itens abertos de identidade nem de distribuição.

Não há bloqueio aberto da `T-FV-01` à `T-FV-12`. A revisão de segurança da tarefa 12 não acrescentou bloqueio. A revisão de segurança da tarefa 11 deixou o ramo `ABERTA` ainda poder gravar quente, UF e cidade de um lead excluído da mesma empresa. Ganho e perdido desse lead são recusados. Isso não permanece como bloqueio. A revisão de segurança da tarefa 10 deixou um achado não bloqueante: vendedor que não é o dono de um lead final recebe a mensagem de situação final. A revisão de código deixou `updatedAt` com `Instant.now()` em `comGanho` e `comPerdido`, sem efeito no critério. A revisão de segurança da tarefa 8 deixou um achado não bloqueante: `TenantContext.role()` usa o primeiro papel do conjunto. A da tarefa 3 deixou a checagem de papel `ADMIN` na rota da tarefa 7. A da tarefa 4 deixou `salvar` de negócio e de histórico sem reconferir a empresa. A primeira `./mvnw test` da tarefa 2 falhou com o Docker padrão inativo; a repetição naquela sessão usou o socket do Docker Desktop e passou. Isso não permanece como bloqueio.

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
