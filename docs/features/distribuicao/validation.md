# Validação — Distribuição

## 1. Identificação da spec avaliada

- **Spec:** `docs/features/distribuicao/spec.md`
- **Discovery:** `docs/features/distribuicao/discovery.md`, aprovado em 16 de outubro de 2026, com a seção «Decisões posteriores» do mesmo dia
- **Feature:** Distribuição (`distribuicao`)
- **Data da avaliação:** 16 de outubro de 2026

Esta é a segunda avaliação. A primeira, também de 16 de outubro de 2026, terminou em **REPROVADA**. O usuário autorizou substituir este arquivo depois que a spec e o discovery foram corrigidos. A spec continua declarando que não está validada. Esta avaliação não reescreve a spec e não produz plano.

## 2. Resumo da avaliação

A spec pode servir de base de planejamento. Os quatro bloqueios da avaliação anterior e a unidade de «N dias» estão decididos no discovery e repetidos na spec, sem contradição entre RN e a seção 10.

Permanecem ressalvas que não mudam dono, autorização nem a persistência da troca. Decisão: **APROVADA COM RESSALVAS**.

## 3. Problemas bloqueantes

Não houve achado nesta categoria.

O conjunto do lote geral está na RF-DS-03 e nas decisões posteriores. A troca manual de lead `ABERTA` que já tem dono está na RF-DS-02. A confirmação que executa a regra de novo está na RN-DS-17, e a seção 10 não a trata mais como dúvida. A automática `FILA` está na RN-DS-16 e no discovery. O prazo de parado é duração de N períodos de 24 horas na RN-DS-10.

## 4. Problemas importantes

Não houve achado nesta categoria.

Nenhum ponto em aberto muda quem fica com o lead, quem pode executar a operação ou se a troca de dono permanece gravada.

## 5. Melhorias recomendadas

### 5.1 Texto do e-mail

- **Classificação:** comprovado.
- **Onde:** seção 3, fora de escopo, e seção 10.
- **O que falta:** assunto e corpo não foram aprovados.
- **Por que não bloqueia:** destinatário, momento, falha que não desfaz a troca e registro persistido da falha estão decididos. O texto não altera dono nem histórico.

### 5.2 Carga de `LeadAtribuido`

- **Classificação:** comprovado.
- **Onde:** seção 8, decisões técnicas propostas, e seção 10.
- **O que falta:** os campos do evento são proposta, não requisito.
- **Por que não bloqueia:** a obrigação de publicar a cada troca está aprovada. Não há consumidor deste evento em `src`. O plano pode adotar ou trocar a proposta sem reabrir produto.

### 5.3 Intervalo da verificação de parado

- **Classificação:** comprovado.
- **Onde:** fluxo «Marcar parado» e seção 8.
- **O que falta:** o intervalo entre verificações não está aprovado. A spec permite que um teste invoque a verificação diretamente.
- **Por que não bloqueia:** a duração que marca o lead está decidida. O plano escolhe quando a verificação roda.

### 5.4 Paginação da lista sem dono

- **Classificação:** comprovado.
- **Onde:** seção 10.
- **O que falta:** paginação e ordem dessa lista não foram aprovadas.
- **Por que não bloqueia:** os filtros da RF-DS-01 estão decididos. A ordenação da fila de retirada é outra regra, a RN-DS-08, e está testável.

## 6. Critérios de aceitação ausentes ou insuficientes

### 6.1 Correspondência de cidade e UF

- **Classificação:** comprovado.
- **Onde:** RN-DS-19, sem critério correspondente na seção 9.
- **O que falta:** nenhum CA observa espaço nas pontas, acento, caixa da cidade ou UF gravada em maiúsculas.
- **Por que não bloqueia:** a regra está observável. O critério pode ser acrescentado depois sem mudar a decisão de dono.

Os critérios CA-04, CA-06, CA-12, CA-13, CA-15, CA-18 e CA-19 cobrem os comportamentos que a avaliação anterior apontou como lacuna.

## 7. Divergências entre discovery, spec e implementação atual

### 7.1 Decisões posteriores

- **Classificação:** comprovado.
- **Onde:** seção «Decisões posteriores» do discovery e RF-DS-02, RF-DS-03, RN-DS-10, RN-DS-16, RN-DS-17, RN-DS-19 e RN-DS-20.
- **Resultado:** a spec repete essas decisões. A frase antiga do discovery que deixava o lote geral e a atribuição manual indefinidos está marcada como substituída. Não há divergência restante nesses pontos.

### 7.2 Afirmações técnicas inspecionadas

Não houve afirmação técnica inspecionada que o código tenha desmentido.

Reconfirmado nesta avaliação:

- `DesativarUsuarioUseCase` só menciona `lead` no comentário de que não escreve nessa tabela.
- `V2__criar_tabelas_empresa_e_usuarios.sql` não tem coluna de fuso. A RN-DS-19 usa isso para não tratar o prazo como dia de calendário.
- As afirmações já conferidas na avaliação anterior sobre `Lead`, `ImportarLeadsUseCase`, `CsvLeads`, `OrigemLead`, papéis do JWT, `ErroResponse`, `envio_email`, `EmailParaEnviar`, ausência de `LeadAtribuido` e de `quarkus-scheduler` continuam válidas: o código da aplicação não mudou desde então.

Não verificado, e a spec já registra a limitação: não há código Angular neste repositório. Isso não foi tratado como problema nem como ausência de problema.

## 8. Decisão final e justificativa

**APROVADA COM RESSALVAS.**

Não há problema bloqueante nem problema importante que precise ser corrigido antes do planejamento. As ressalvas são o texto do e-mail, a carga proposta de `LeadAtribuido`, o intervalo da verificação de parado, a paginação da lista e a falta de um critério para a RN-DS-19. Nenhuma delas altera dono, autorização ou a persistência da troca.

O planejamento ainda depende de aprovação explícita desta validação. Esta avaliação não o inicia.
