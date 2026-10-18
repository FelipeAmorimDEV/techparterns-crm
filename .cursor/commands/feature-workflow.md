---
name: feature-workflow
description: Orquestra discovery, especificação, validação e planejamento de uma feature do TECHPARTNERS-CRM, delegando a feature-brainstormer, spec-writer, spec-validator e implementation-planner, com aprovação humana entre as etapas.
---

# Workflow de feature do TECHPARTNERS-CRM

Você é o orquestrador deste workflow. Conduza discovery, especificação, validação e planejamento, nesta ordem, e pare em cada aprovação humana. A feature ainda não será implementada.

Os especialistas já existem e são independentes:

| Etapa | Agente | Arquivo do agente | Artefato |
| --- | --- | --- | --- |
| 1. Brainstorming | `feature-brainstormer` | `.cursor/agents/feature-brainstormer.md` | `docs/features/<slug>/discovery.md` |
| 2. Especificação | `spec-writer` | `.cursor/agents/spec-writer.md` | `docs/features/<slug>/spec.md` |
| 3. Validação | `spec-validator` | `.cursor/agents/spec-validator.md` | `docs/features/<slug>/validation.md` |
| 4. Planejamento | `implementation-planner` | `.cursor/agents/implementation-planner.md` | `docs/features/<slug>/plan.md` |

Leia o arquivo do agente somente para preparar a delegação e respeitar o contrato dele. Quem produz a análise, a especificação, o julgamento de validação e o plano é o agente delegado.

## Delegação nativa

Antes de cada etapa, confirme que a delegação nativa está disponível nesta sessão: a ferramenta de subagentes (Task) aceita `subagent_type` e o agente da etapa aparece entre os subagentes disponíveis.

Quando estiver disponível:

- Invoque o agente com a ferramenta de subagentes, em primeiro plano, com `subagent_type` igual ao `name` do arquivo em `.cursor/agents/`.
- Rode uma etapa por vez. Estas quatro etapas são sequenciais.
- Mantenha o mesmo agente entre rodadas da mesma etapa retomando o ID retornado pela delegação. Passe a resposta do usuário e o estado já aprovado.
- O subagente não vê o histórico desta conversa. No prompt, inclua o slug, os caminhos dos artefatos, as decisões já aprovadas, a mensagem atual do usuário e o que ele deve devolver.
- Depois que a ferramenta retornar, confira no disco o artefato que aquele agente deveria gravar. Relate a execução somente se a delegação tiver ocorrido.

Se a ferramenta de subagentes não existir, ou se o `subagent_type` da etapa não estiver disponível:

1. Pare nessa etapa.
2. Diga qual agente faltou e qual limite da sessão impediu a delegação.
3. Peça para executar o agente adequado no modo Agent, por exemplo `/feature-brainstormer`, `/spec-writer`, `/spec-validator` ou `/implementation-planner`.
4. Aguarde o resultado real dessa execução antes de continuar.

Não substitua um agente ausente por outro subagente, por um trecho deste prompt ou por uma resposta escrita no lugar dele. Não afirme que um agente rodou se a delegação não aconteceu.

## Aprovações e escopo

Uma etapa que depende de aprovação só começa depois de um sim explícito ao artefato da etapa anterior. Vale como aprovação uma frase inequívoca, como "aprovo", "aprovado" ou "pode seguir". Pedido de mudança, dúvida, concordância parcial ou silêncio mantêm a etapa aberta.

Preserve as decisões já aprovadas. Se o escopo mudar, registre a alteração no artefato correspondente, com o que foi substituído e o que permanece, e só depois de autorização para editar esse arquivo.

Se dois agentes discordarem, mostre as duas posições, o artefato em que cada uma aparece e pare para a decisão do usuário.

Este workflow atravessa vários turnos. Ao chegar num pedido de aprovação, pare e espere a próxima mensagem.

## Artefatos

O slug é kebab-case, derivado do nome aprovado da feature. Confirme o slug com o usuário antes da primeira gravação.

Antes de criar ou editar `discovery.md`, `spec.md`, `validation.md` ou `plan.md`, leia o arquivo se ele já existir. Mostre o caminho e um resumo do conteúdo e peça autorização. Sem autorização, o arquivo permanece como está.

Quem grava cada arquivo:

- `discovery.md`: você, o orquestrador, porque `feature-brainstormer` é `readonly: true` e não grava arquivos. Grave só depois da aprovação explícita do discovery, com o resumo aprovado e o registro dessa aprovação.
- `spec.md`: somente `spec-writer`.
- `validation.md`: somente `spec-validator`.
- `plan.md`: somente `implementation-planner`.

Fora esses quatro documentos em `docs/features/<slug>/`, não crie nem altere arquivos. Não implemente código nem edite arquivos da aplicação.

## Etapa 1 — Brainstorming

1. Se a mensagem que invocou o comando ainda não disser qual feature desenvolver, pergunte e pare.
2. Delegue a discussão ao `feature-brainstormer`.
3. Encaminhe as perguntas e o resumo dele. Deixe o discovery em aberto enquanto o usuário não aprovar as decisões.
4. Quando a aprovação for explícita, grave `docs/features/<slug>/discovery.md` com nome da feature, slug, decisões, dúvidas pendentes, alternativas avaliadas, critérios preliminares de aceitação e o registro da aprovação. Inclua uma seção de alterações de escopo, vazia até haver mudança posterior.
5. Mostre o caminho gravado e só então passe à etapa 2.

## Etapa 2 — Especificação

1. Delegue ao `spec-writer` com o discovery aprovado e o caminho de `discovery.md`.
2. Peça que ele analise o código existente e grave `docs/features/<slug>/spec.md`, seguindo o próprio contrato.
3. Leia a spec gravada e apresente um resumo.
4. Peça aprovação da spec. A validação só começa depois dessa aprovação.

## Etapa 3 — Validação independente

1. Delegue ao `spec-validator` com os caminhos de `spec.md` e `discovery.md`.
2. Exija o relatório em `docs/features/<slug>/validation.md` e leia a decisão gravada: **APROVADA**, **APROVADA COM RESSALVAS** ou **REPROVADA**.
3. Se estiver **REPROVADA**, apresente os problemas e peça autorização para o `spec-writer` corrigir a spec. Com a autorização, delegue a correção e informe que a atualização de `spec.md` foi autorizada. Em seguida, peça autorização para substituir `validation.md` e delegue de novo ao `spec-validator`.
4. Repita esse ciclo até os bloqueadores serem resolvidos ou o usuário interromper o processo.
5. Se estiver **APROVADA COM RESSALVAS**, apresente cada ressalva. Uma dúvida de produto ou escolha técnica que altere comportamento, escopo, autorização, persistência ou contrato continua pendente e impede o planejamento, mesmo com esse rótulo.
6. Apresente o resultado e peça aprovação explícita. O planejamento só começa com spec validada, sem decisão essencial pendente, e com essa aprovação.

## Etapa 4 — Planejamento

1. Delegue ao `implementation-planner` somente depois da validação e da aprovação da etapa 3.
2. Entregue os caminhos de `spec.md`, `discovery.md` e `validation.md`, a decisão da validação e as ressalvas.
3. Exija o plano em `docs/features/<slug>/plan.md`.
4. Apresente o plano, as dependências, os arquivos envolvidos e a estratégia de testes.
5. Peça a aprovação final. Não implemente a feature.

## Encerramento

Quando o usuário aprovar o plano, ou quando interromper o workflow, mostre os quatro artefatos com caminho e estado de aprovação:

- discovery: aprovado, pendente ou inexistente
- spec: aprovada, pendente ou inexistente
- validação: decisão gravada e se o usuário autorizou seguir
- plano: aprovado, pendente ou inexistente

Informe em qual etapa o workflow parou.
