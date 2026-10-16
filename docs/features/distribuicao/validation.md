# Validação — Distribuição

## 1. Identificação da spec avaliada

- **Spec:** `docs/features/distribuicao/spec.md`
- **Discovery:** `docs/features/distribuicao/discovery.md`, marcado como aprovado em 16 de outubro de 2026
- **Feature:** Distribuição (`distribuicao`)
- **Data da avaliação:** 16 de outubro de 2026

A spec existe e declara o mesmo discovery como entrada. O discovery está aprovado. A spec informa que ela própria não está validada. Esta avaliação não reescreve a spec e não produz plano.

## 2. Resumo da avaliação

A spec não pode servir de base de planejamento. Ela acompanha a maior parte das decisões aprovadas e separa proposta técnica de requisito, mas deixa em aberto comportamentos que mudam quem recebe o lead e, ao mesmo tempo, transforma uma dessas aberturas em regra.

Decisão: **REPROVADA**.

## 3. Problemas bloqueantes

### 3.1 Conjunto de entrada do lote geral

- **Classificação:** comprovado.
- **Onde:** RF-DS-03 e a seção 10, item «Conjunto do lote geral».
- **O que falta:** o discovery aprova distribuir em lote por rodízio, peso, carga ou território, com simulação antes de confirmar. Não diz se o lote percorre todos os leads sem dono da empresa, só uma seleção ou só o resultado dos filtros.
- **Por que bloqueia:** duas implementações podem atribuir conjuntos diferentes e ainda assim cumprir o restante da spec. A redistribuição de carteira não sofre essa lacuna: o conjunto dela está fechado na RF-DS-08.
- **Pergunta:** o lote geral usa todos os leads sem dono da empresa, apenas os leads escolhidos pelo admin, ou apenas os leads que passaram pelos filtros da lista?

### 3.2 Atribuição manual de lead que já tem dono

- **Classificação:** comprovado.
- **Onde:** RF-DS-02, o fluxo «Atribuir seleção» e a seção 10, item «Atribuição manual de lead que já tem dono».
- **O que falta:** o discovery aprova atribuir leads selecionados a um vendedor e aprova a redistribuição da carteira por lote. Não diz se a atribuição manual pode trocar um dono já existente.
- **Por que bloqueia:** permitir a troca e recusá-la são comportamentos diferentes de dono e de histórico.
- **Pergunta:** a atribuição manual pode trocar o dono de um lead `ABERTA` que já tem outro dono, ou só atribui lead sem dono?

### 3.3 Confirmação do lote é regra e, ao mesmo tempo, dúvida

- **Classificação:** comprovado.
- **Onde:** RN-DS-17 e a seção 10, item «Simulação como snapshot».
- **O que está errado:** RN-DS-17 exige que a confirmação execute a regra de novo e não use a simulação como reserva. A seção 10 diz que nenhuma decisão manda guardar a simulação e que a leitura «não bloqueia». O discovery só aprova ver a simulação antes de confirmar e gravar na confirmação. Não escolhe entre repetir a regra e aplicar o resultado mostrado.
- **Por que bloqueia:** a spec promove uma escolha não aprovada a regra e, na seção seguinte, trata essa escolha como interpretação. O planejamento não sabe se deve implementar a RN-DS-17 ou esperar a decisão. O resultado observável muda se o conjunto ou a elegibilidade mudarem entre a simulação e a confirmação.
- **Pergunta:** a confirmação reaplica a regra no momento em que o admin confirma, ou grava exatamente o destino mostrado na simulação?

### 3.4 Regra automática `FILA` definida sem decisão

- **Classificação:** comprovado.
- **Onde:** RN-DS-16, RF-DS-07 e CA-04.
- **O que está errado:** o discovery inclui `FILA` entre os valores de `regra_padrao` e diz que a automática de lead novo segue a regra escolhida. Não descreve o que `FILA` faz na criação. A spec exige que o lead novo permaneça sem dono e sem linha de histórico.
- **Por que bloqueia:** outra leitura possível é a automática entregar o lead ao vendedor que pegaria o primeiro da fila. As duas mudam dono, histórico e `LeadAtribuido`. A spec escolheu uma delas sem decisão aprovada.
- **Pergunta:** com a automática ligada e regra `FILA`, o lead novo fica sem dono até um vendedor pegá-lo, ou a feature já o atribui a um vendedor no momento da criação?

## 4. Problemas importantes

### 4.1 «Mais de N dias» não tem unidade observável

- **Classificação:** comprovado.
- **Onde:** RN-DS-10 e CA-18.
- **O que falta:** o discovery e a spec dizem «há mais de N dias», com padrão 5, contados desde a última atribuição. Não dizem se o dia é um período de 24 horas ou um dia de calendário, nem qual fuso vale no segundo caso.
- **Por que importa:** CA-18 pode passar ou falhar conforme o relógio escolhido. Precisa ser decidido antes de planejar a marcação de parado. Não responde no lugar de quem decide.
- **Pergunta:** N dias são N períodos de 24 horas a partir de `atribuido_em`, ou dias de calendário? Se forem dias de calendário, qual fuso?

## 5. Melhorias recomendadas

### 5.1 Ordem do histórico

- **Classificação:** comprovado.
- **Onde:** fluxo «Histórico».
- **O que aparece:** a spec exige linhas da mais antiga para a mais nova. O discovery aprova o admin ver o histórico e não fixa a ordem.
- **Por que é melhoria:** não muda quem é o dono. Uma ordem estável ajuda o teste, mas não estava aprovada.

### 5.2 Empresa suspensa

- **Classificação:** hipotético.
- **Onde:** a spec não trata `company.status`.
- **O que se observou:** `src/main/resources/db/migration/V2__criar_tabelas_empresa_e_usuarios.sql` admite `ACTIVE` e `SUSPENDED`. `ImportarLeadsUseCase` não consulta esse status. Não há evidência de que a distribuição precise bloqueá-lo.
- **Por que não é defeito:** o risco é plausível e não está demonstrado. Não deve ser tratado como requisito faltante.

## 6. Critérios de aceitação ausentes ou insuficientes

### 6.1 Falha de e-mail sem registro observável

- **Classificação:** comprovado.
- **Onde:** CA-19, comparado com a decisão de parado no discovery e com o requisito não funcional da spec sobre a falha ficar registrada.
- **O que falta:** CA-19 verifica que o lead continua sem dono e que a linha `PARADO` permanece. Não verifica que a falha ficou registrada. O discovery aprova exatamente esse registro.
- **Por que é insuficiente:** uma implementação que não registra a falha passa no CA-19.

### 6.2 Ordem interna da fila

- **Classificação:** comprovado.
- **Onde:** CA-15, comparado com RN-DS-08.
- **O que falta:** CA-15 cobre quente na frente e devolvido ou parado não quente atrás. Não cobre a ordem do criado há mais tempo ao mais novo dentro de cada grupo, nem que quente devolvido permaneça no grupo quente.
- **Por que não bloqueia sozinho:** a RN-DS-08 já descreve essa ordem de forma observável. O critério é mais estreito que a regra.

### 6.3 Peso zero nas outras regras

- **Classificação:** comprovado.
- **Onde:** CA-09, comparado com RN-DS-04.
- **O que falta:** CA-09 verifica a proporção e que peso 0 não recebe pela regra de peso. Não verifica que peso 0 ainda pode receber por outra regra ou por atribuição manual, frase que a própria RN-DS-04 afirma.
- **Por que não bloqueia sozinho:** a regra está escrita. O critério não a cobre por inteiro.

## 7. Divergências entre discovery, spec e implementação atual

### 7.1 `FILA` automática

- **Classificação:** comprovado.
- **Onde:** RN-DS-16 e CA-04, contra a tabela de `distribution_settings` do discovery.
- **Divergência:** o discovery nomeia o valor e não o efeito. A spec fixa o efeito «permanece sem dono». É o mesmo problema bloqueante 3.4.

### 7.2 Simulação e confirmação

- **Classificação:** comprovado.
- **Onde:** RN-DS-17 contra a seção 10 da própria spec e contra o discovery, que só aprova simular antes de gravar.
- **Divergência:** a spec contém uma regra e uma pendência sobre o mesmo comportamento. É o problema bloqueante 3.3.

### 7.3 Ordem do histórico

- **Classificação:** comprovado.
- **Onde:** fluxo «Histórico».
- **Divergência:** ordem ascendente não está no discovery. É a melhoria 5.1, não um bloqueio.

### 7.4 Afirmações técnicas inspecionadas

Não houve afirmação técnica inspecionada que o código tenha desmentido.

Confirmado neste repositório:

- A tabela `lead` e `Lead.java` não têm dono, situação, UF, cidade nem quente. A única chamada de `Lead.tryCreate` está em `ImportarLeadsUseCase`. O CSV em `CsvLeads` só tem `nome_contato`, `telefone`, `whatsapp` e `email`. `OrigemLead` só tem `IMPORTACAO`.
- `ImportarLeadsUseCase` compara `PAPEL_ADMIN` com o literal `"admin"` e não lê `TenantContext.role()`. `ExcluirLeadUseCase` compara `tenant.role()` com `"admin"`. `EmissorAccessTokenJwt` coloca `papel.name()` em `groups`. `LeadResource` não declara `@RolesAllowed`.
- `DesativarUsuarioUseCase` não referencia lead.
- `ErroResponse` tem `path`, `mensagem`, `codigoErro` e `dataHora`. `DomainException` mapeia para HTTP 400 e `REQUISICAO_INVALIDA`. `RecursoNaoEncontradoException` mapeia para HTTP 404. `SegurancaExceptionMappers` mapeia acesso negado para HTTP 403 e `ACESSO_NEGADO`. `GlobalExceptionHandler` mapeia SQLState `23505` para HTTP 409 e `CONFLITO`.
- `envio_email.tipo` só admite `SETUP` e `RESET` em `V4__criar_tabelas_auditoria_envio_e_login.sql`. `EnvioEmailListener` observa `EmailParaEnviar` em `TransactionPhase.AFTER_SUCCESS`. Não há tipo `LeadAtribuido` em `src`. O `pom.xml` não declara `quarkus-scheduler`. As migrações encontradas vão de `V1` a `V4`. O datasource configurado é PostgreSQL.
- `app_user.created_at` e `app_user.email` existem em `V2__criar_tabelas_empresa_e_usuarios.sql`.

Não verificado, e a spec já registra a limitação: não há código Angular neste repositório. A URL de frontend e o CORS em `application.properties` não comprovam nem afastam impacto de tela. Isso não foi tratado como problema nem como ausência de problema.

## 8. Decisão final e justificativa

**REPROVADA.**

Há quatro problemas bloqueantes: o conjunto do lote geral, a atribuição manual de lead que já tem dono, a contradição entre RN-DS-17 e a seção 10, e o efeito não aprovado da regra automática `FILA`. Qualquer um deles impede usar a spec como base de planejamento. A unidade de «N dias» é importante e também precisa de decisão antes de planejar o parado. As melhorias e os critérios estreitos não são a causa da reprovação.

As perguntas deste relatório não foram respondidas aqui. Corrigi-las é escolha de produto e, depois, atualização da spec pelo `spec-writer`.
