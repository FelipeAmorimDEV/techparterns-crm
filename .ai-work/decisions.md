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
| FV-SEC-08 | Fazer a revisão de segurança da `T-FV-08` | Feita pelo `security-reviewer`. Decisão **SEM BLOQUEIO**. `PATCH /funil/leads/{leadId}` aceita `ADMIN` e `SELLER`. A empresa sai do claim `companyId`. Vendedor que não é o dono não grava. Lead de outra empresa e lead excluído não são alterados. O dono em `lead_ownership` não é escrito |
| FV-T8-DOMINIO | `NegocioFunil.comDadosAbertos` na T-FV-08 | Os campos comerciais eram `final`. O método devolve outra instância e conserva etapa, entrada, plano fechado, valor fechado, motivo e observação |
| FV-T8-PAPEL | `TenantContext.role()` usa o primeiro papel do conjunto | Achado não bloqueante da revisão de segurança. O emissor grava um único grupo e a rota já restringe `ADMIN` e `SELLER`. Não bloqueia a T-FV-08 |
| FV-SEC-09 | Fazer a revisão de segurança da `T-FV-09` | Feita pelo `security-reviewer`. Decisão **SEM BLOQUEIO**. `POST /funil/leads/{leadId}/mover` aceita `ADMIN` e `SELLER`. A empresa sai do claim `companyId`. O admin da mesma empresa move sem conferir dono, como a spec do fluxo já autoriza. Vendedor que não é o dono, lead sem dono, lead de outra empresa e lead excluído não gravam |
| FV-T9-MSG | Um vendedor que não é o dono de um lead já `GANHO` ou `PERDIDO` recebe a mensagem de situação final | Achado não bloqueante da revisão de segurança. O movimento não ocorre e não há histórico nem evento. A mensagem confirma a situação final daquele lead da empresa. Não bloqueia a T-FV-09 |
| FV-T9-HIST | `HistoricoEtapa.tryCreate` roda depois de `negocios.salvar` | Risco não bloqueante da revisão de código. Um `Result` de falha confirmaria a transação sem histórico. No caminho atual o destino e o instante do `Clock` não são nulos, e `tryCreate` só falha nesses dois casos. Não bloqueia a T-FV-09 |
| FV-T9-ADMIN | Não há teste em que o admin não seja o dono | Risco não bloqueante da revisão de código. O código pula a checagem de dono para `ADMIN`, no mesmo formato da tarefa 8 e dentro da autorização da spec. Não bloqueia a T-FV-09 |
| FV-T9-NOW | `NegocioFunil.moverPara` reconstitui `updatedAt` com `Instant.now()` | Risco não bloqueante da revisão de código. O instante da entrada na etapa e do histórico usa o `Clock` injetado. Não bloqueia a T-FV-09 |
| FV-T9-DOMINIO | `NegocioFunil.moverPara` na T-FV-09 | Os campos da posição eram `final`. O método devolve outra instância, troca etapa e instante de entrada, e conserva roteiro, demo, valores, plano, motivo e observação |
| FV-T9-HISTORICO | O `salvar` de histórico continua sem reconferir a empresa | A revisão de segurança desta tarefa não reabre o achado da T-FV-04 como bloqueio. Origem e destino já foram lidos com a empresa do token. Não bloqueia a T-FV-09 |
| FV-SEC-10 | Fazer a revisão de segurança da `T-FV-10` | Feita pelo `security-reviewer`. Decisão **SEM BLOQUEIO**. `POST /funil/leads/{leadId}/ganho` e `/perdido` aceitam `ADMIN` e `SELLER`. A empresa sai do claim `companyId`. O admin da mesma empresa conclui sem ser o dono. Vendedor que não é o dono, lead sem dono, lead de outra empresa, lead excluído e motivo de outra empresa não gravam. A impl não escreve dono |
| FV-T10-MSG | Um vendedor que não é o dono de um lead já final recebe «Situação final não muda» | Achado não bloqueante da revisão de segurança. A mesma ordem da T-FV-09: exclusão, situação final, dono. Não há gravação. Não atravessa empresa. Não bloqueia a T-FV-10 |
| FV-T10-DOMINIO | `NegocioFunil.comGanho` e `comPerdido` na T-FV-10 | Os campos são `final`. Cada método devolve outra instância, troca etapa e instante de entrada e conserva roteiro, demo, valor estimado, plano de interesse e previsão. O ganho grava plano e valor fechados. O perdido grava motivo e observação por inteiro |
| FV-T10-NOW | `comGanho` e `comPerdido` reconstituem `updatedAt` com `Instant.now()` | Risco não bloqueante da revisão de código. O instante do histórico e a entrada na etapa usam o `Clock`. Nenhum critério desta tarefa olha `updatedAt`. Não bloqueia a T-FV-10 |
| FV-SEC-11 | Fazer a revisão de segurança da `T-FV-11` | Feita pelo `security-reviewer`. Decisão **SEM BLOQUEIO**. `PATCH /leads/{id}` aceita `ADMIN` e `SELLER`. A empresa sai do claim `companyId`. Vendedor que não é o dono não grava. Lead de outra empresa e lead já final não são reescritos. Motivo de outra empresa não conclui o perdido. A impl não escreve dono |
| FV-T11-EXCLUSAO | Ganho e perdido pelo `PATCH` recusam lead excluído antes de gravar | A primeira revisão de código pediu correção: `FecharLeadNoFunilImpl` concluía o final e mudava a etapa. A correção consulta `ConsultaLeadFunil` com a empresa do token e lança «Lead não encontrado» se o lead não existe ou está excluído, antes de validar plano ou motivo. A segunda revisão de código ficou **APROVADA**. O ramo `ABERTA` não passou a ler exclusão |
| FV-T11-ABERTA | O ramo `ABERTA` ainda pode gravar quente, UF e cidade de um lead excluído da mesma empresa | Achado não bloqueante da revisão de segurança, depois da correção. O plano manda conservar esse ramo. Não conclui ganho nem perdido e não atravessa empresa. Não bloqueia a T-FV-11 |
| FV-T11-FATOS | Método novo em `AtualizarFatosLead` na T-FV-11 | O método da tarefa 10 só grava a situação e conserva quente, UF e cidade. Uma segunda `comFatos` depois do final falha e confirmaria a etapa se a falha fosse só `Result.fail`. O método novo grava os quatro fatos numa chamada e não escreve dono |
| FV-T11-MSG | Vendedor que não é o dono de um lead já final recebe «Situação final não muda» | Achado não bloqueante. A mesma ordem das tarefas 9 e 10. Não há gravação. Não bloqueia a T-FV-11 |
| FV-T11-UF | UF ou cidade omitidas passam em `ValidacaoDistribuicao` como nulo | Risco não bloqueante da revisão de código. Se o ganho ou o perdido conclui, esse nulo é gravado. O plano mandou usar essa função. Os testes de sucesso enviam os dois campos. Não bloqueia a T-FV-11 |
| FV-SEC-12 | Fazer a revisão de segurança da `T-FV-12` | Feita pelo `security-reviewer`. Decisão **SEM BLOQUEIO**. `GET /funil/quadro` aceita `ADMIN` e `SELLER`. A empresa sai do claim `companyId`. A query só tem `uf` e `quente`. Admin vê a empresa; vendedor vê só o lead de que é dono. Lead excluído não aparece. A resposta não leva nome, telefone, e-mail, plano, roteiro, cultura nem tag |
| FV-T12-LISTAR | `NegocioFunilRepository.listarPorEmpresa` na T-FV-12 | O repositório só tinha leitura por lead. O quadro precisa dos negócios da empresa para `ValorColuna` e `AtrasoEtapa`. O JPQL filtra etapa e lead pela empresa recebida, que é a do token |
| FV-T12-LEITURA | Uma consulta de lead por negócio | Risco não bloqueante da revisão de código. Não vaza empresa e não altera quantidade, valor nem atraso. Não bloqueia a T-FV-12 |
