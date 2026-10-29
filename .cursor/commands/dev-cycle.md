---
name: dev-cycle
description: Retoma o ciclo de desenvolvimento do TECHPARTNERS-CRM a partir de .ai-work, delegando aos agentes de .cursor/agents sem implementar tarefa bloqueada.
---

# Ciclo de desenvolvimento

Siga `.cursor/rules/orchestration.mdc`.

1. Leia `.ai-work/execution-plan.md`, `progress.md`, `decisions.md` e `blocked.md`.
2. Confira `git status` e se os arquivos citados ainda existem.
3. Escolha a próxima tarefa `READY` que não dispute arquivo com outra tarefa em andamento e que não dependa de item `OPEN`.
4. Invoque o agente dono da tarefa com a ferramenta de subagente, `subagent_type` igual ao `name` em `.cursor/agents/`. Se esse tipo não estiver na lista da sessão, pare e informe a limitação.
5. Atualize o estado com o resultado real. Não marque `DONE` sem o quality gate.

Não implemente feature neste comando se a tarefa ainda estiver `PENDING` ou `BLOCKED`. Não invente requisito. Não exponha credencial.

Se o pedido for o comando `feature-workflow`, esse comando prevalece e para antes da implementação.
