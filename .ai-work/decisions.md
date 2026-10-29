# Decisões

`APROVADA` aqui só quando esta sessão ou um arquivo citado mostrar aprovação humana. O restante é `PROPOSTA`.

## Já registradas nas fontes de feature

As aprovações de discovery em 16/10/2026 e as decisões dos `validation.md` estão nos próprios arquivos em `docs/features/`. Esta configuração não as reabre e não as reconfirma. Resumo do que esses arquivos dizem:

| Fonte | O que o arquivo afirma |
| --- | --- |
| `docs/features/identidade-e-acesso/validation.md` | APROVADA COM RESSALVAS. DEL-18 a DEL-21 ainda dependem do usuário |
| `docs/features/leads-e-importacao/validation.md` | APROVADA |
| `docs/features/distribuicao/validation.md` | APROVADA COM RESSALVAS. O planejamento ainda pedia aprovação explícita da validação |

## Desta configuração

| ID | Decisão | Estado |
| --- | --- | --- |
| CFG-01 | Manter `.claude/skills/` como referência. Não criar pasta `skills/` nem copiar as skills | Adotada nesta sessão como instrução do pedido. Não é decisão de negócio |
| CFG-02 | Manter os agentes `feature-brainstormer`, `spec-writer`, `spec-validator` e `implementation-planner` e o comando `feature-workflow` | Adotada nesta sessão. Arquivos não foram reescritos |
| CFG-03 | Usar `specs/` como índice e rastreio, sem transcrever as regras de `docs/features/` | `PROPOSTA` operacional já aplicada nos arquivos. Uma projeção posterior precisa do `spec-generator` e não pode virar segunda norma conflitante |
| CFG-04 | Não alterar `.gitignore`, que ignora `docs/` | `PROPOSTA`. Mudar isso versiona as specs e é decisão de repositório, não tomada aqui |
| CFG-05 | Não tratar o POM como multi-módulo | Fato lido do `pom.xml`. Não há proposta de quebrar o reator |
| CFG-06 | Não executar `docs/superpowers/plans/2026-10-17-distribuicao-no-padrao.md` nesta sessão | `PROPOSTA` de espera. Falta autorização |

## Funil de vendas

| ID | Decisão | Estado |
| --- | --- | --- |
| FV-PLAN | Implementar o funil pelas 12 tarefas de `docs/features/funil-de-vendas/plan.md`, aprovado em 28/10/2026. A validação está APROVADA COM RESSALVAS, sem bloqueio. Não reabrir discovery, spec nem validação | Autorizada pelo pedido desta sessão, que cita o plano já aprovado |
| FV-FORA | Não implementar o que a seção «Fora do plano do funil» de `.ai-work/execution-plan.md` lista, inclusive tela, terceira casa decimal, corpo de evento, paginação e a spec da Distribuição | Limite do plano. Não é decisão nova |
| FV-SEC-01 | Dispensar a revisão de segurança da `T-FV-01` | Adotada pelo orquestrador. A tarefa não tem autenticação, autorização, borda de tenant, dado pessoal, segredo, upload nem integração |
| FV-SEC-02 | Fazer a revisão de segurança da `T-FV-02` | Feita pelo `security-reviewer`. Decisão **SEM BLOQUEIO**. A tarefa lê lead e filtra empresa; não há resource HTTP novo |
| FV-SEC-03 | Fazer a revisão de segurança da `T-FV-03` | Feita pelo `security-reviewer`. Decisão **SEM BLOQUEIO**. `execute` lê a empresa do token; `listarPorEmpresa` filtra `company_id`. Não há resource nesta tarefa |
| FV-T3-LISTAR | Acrescentar `EtapaRepository.listarPorEmpresa` na T-FV-03 | Necessário para os critérios de etapa já gravada. O plano dizia repositórios da tarefa 2 sem edição, e eles não tinham leitura por empresa. Só esse método entrou. V6 não mudou |
| FV-T3-PAPEL | `GarantirEtapasUseCase.execute` não consulta o papel | Achado não bloqueante da revisão de segurança. A rota e a recusa de quem não é admin ficam na tarefa 7, antes de chamar `execute`. Não bloqueia a T-FV-03 |
| FV-SEC-04 | Fazer a revisão de segurança da `T-FV-04` | Feita pelo `security-reviewer`. Decisão **SEM BLOQUEIO**. Só o admin passa do `ImportarLeadsUseCase`; a empresa sai do token e a impl usa `lead.companyId()` |
| FV-T4-SALVAR | `salvar` de negócio e de histórico não confere de novo se etapa e lead são da mesma empresa | Achado não bloqueante da revisão de segurança. Nesta tarefa o único chamador monta o lead com a empresa do token e escolhe a etapa em `listarPorEmpresa`. Outro chamador futuro precisa de um lead já criado com essa empresa. Não bloqueia a T-FV-04 |
| FV-SEC-05 | Fazer a revisão de segurança da `T-FV-05` | Feita pelo `security-reviewer`. Decisão **SEM BLOQUEIO**. A subida não recebe empresa do cliente; a etapa sai da empresa da linha do lead; lead excluído fica de fora |
| FV-T5-PORTA | Porta `LeadsSemLinhaFunil` e command vazio na T-FV-05 | O plano não listou os dois. `UseCase` exige input e nenhum repositório listava, em todas as empresas, lead não excluído sem linha em `funil_negocio`. A empresa não vai no command |
| FV-T5-LOG | Se `execute` passar a devolver falha, `ColocacaoFunilNaSubida` loga `resultado.getErrors()` | Achado não bloqueante. Hoje `execute` sempre devolve sucesso e a falha de cada lead só loga o tipo da exceção. Não bloqueia a T-FV-05 |
| FV-SEC-06 | Fazer a revisão de segurança da `T-FV-06` | Feita pelo `security-reviewer`. Decisão **SEM BLOQUEIO**. Escrita só com `ADMIN` na rota; empresa do claim `companyId`; listagem e `PATCH` filtram essa empresa |
| FV-T6-LISTAR | `ListarMotivosPerdaUseCase` e os métodos `existeNome` e `listarPorEmpresa` na T-FV-06 | O plano nomeou só cadastrar e alterar. O critério pede nome único antes de gravar e listagem da empresa do token. V6 não mudou |
| FV-T6-PAPEL | Os casos de uso de motivo não consultam o papel | Achado não bloqueante da revisão de segurança. `POST` e `PATCH` têm `@RolesAllowed("ADMIN")`. `GET` aceita `ADMIN` e `SELLER`. Não bloqueia a T-FV-06 |
| FV-T6-PATCH | Não há teste de `PATCH` com id de motivo de outra empresa | Achado não bloqueante. `buscarPorId` exige id e `company_id`; a ausência lança `DomainException` e não grava a outra linha. Não bloqueia a T-FV-06 |
| FV-SEC-07 | Fazer a revisão de segurança da `T-FV-07` | Feita pelo `security-reviewer`. Decisão **SEM BLOQUEIO**. Escrita só com `ADMIN` na rota; empresa do claim `companyId`; etapa de outra empresa não é alterada nem apagada; apagar é lógico e o histórico não cai por `ON DELETE CASCADE` |
| FV-T7-ETAPA | `Etapa.alterar` e `Etapa.desativar` na T-FV-07 | Os campos eram `final`. Renomear, reordenar e gravar probabilidade, cor e SLA exigem mutação sem trocar id, tipo nem flags. Não há reativar |
| FV-T7-NEGOCIO | `NegocioFunilRepository.existeNaEtapa` na T-FV-07 | Nenhum método dizia se a etapa tem linha em `funil_negocio`. O caso de uso já carregou a etapa por id e empresa. O método não filtra empresa de novo |
| FV-T7-PAPEL | Os casos de uso de etapa não consultam o papel | Achado alinhado a FV-T3-PAPEL e FV-T6-PAPEL. As quatro rotas têm `@RolesAllowed("ADMIN")`. Não bloqueia a T-FV-07 |
| FV-T7-APAGAR | Apagar etapa vazia não recusa a única `ABERTO`, `GANHO` ou `PERDIDO` | Achado não bloqueante da revisão de código. O critério recusa apagar quando há lead e recusa desativar a última aberta e as finais únicas. A ordem da linha com `deleted_at` continua ocupada. Não bloqueia a T-FV-07 |
| FV-T7-DESATIVAR | O teste de outra empresa não chama `POST /{id}/desativar` | Achado não bloqueante da revisão de segurança. O desativar usa o mesmo `buscar` de alterar e apagar, que filtra `company_id`. Não bloqueia a T-FV-07 |
