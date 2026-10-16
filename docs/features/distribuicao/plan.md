# Plano — Distribuição

## 1. Objetivo e resumo da solução

Implementar, no backend Quarkus deste repositório, a fonte de verdade do dono do lead e o histórico de cada troca, como a spec validada exige. Não há implementação neste plano.

A solução proposta separa dois módulos. O lead existente ganha situação, UF, cidade e quente. Um módulo novo, `tech.techparterns.distribuicao`, guarda dono, histórico, configuração, território e as regras. A importação de lead novo chama a distribuição automática por uma porta. A desativação de usuário não passa a mover lead.

Frontend Angular fica de fora: não há código Angular neste repositório, e a validação não verificou tela.

## 2. Pré-requisitos e decisões aprovadas

- **Spec:** `docs/features/distribuicao/spec.md`
- **Validação:** `docs/features/distribuicao/validation.md`
- **Decisão:** **APROVADA COM RESSALVAS**, sem pendência bloqueante
- **Discovery:** `docs/features/distribuicao/discovery.md`, aprovado, com a seção «Decisões posteriores»

A implementação segue somente o que a spec exige, inclusive as decisões posteriores: lote geral filtrado, troca manual de lead `ABERTA` que já tem dono, confirmação que executa a regra de novo, automática `FILA` sem dono, prazo em períodos de 24 horas, comparação de cidade e UF, limite zero e histórico da linha mais antiga para a mais nova.

Ressalvas que restringem o plano e não viram decisão aprovada:

- Assunto e corpo do e-mail não estão aprovados. A tarefa do envio usa texto provisório só para o e-mail existir. Trocar esse texto não faz parte do comportamento aprovado.
- Os campos de `LeadAtribuido` não são requisito. O plano adota a carga proposta na seção 8 da spec como escolha técnica desta implementação.
- O intervalo da verificação de parado não está aprovado. O plano não agenda a verificação e não adiciona `quarkus-scheduler`.
- Paginação e ordem da lista sem dono não estão aprovadas. A lista devolve o conjunto filtrado, sem página.
- Não há CA para a RN-DS-19. A regra está aprovada; o teste da tarefa correspondente a verifica mesmo sem número de CA.

Não entra no plano: funil, score, feed de atividades, reabrir `GANHO` ou `PERDIDO`, corrigir a comparação `PAPEL_ADMIN.equals("admin")` em `ImportarLeadsUseCase`, e qualquer refatoração fora da feature.

## 3. Arquitetura e fluxo afetado

Hoje o lead é criado só em `src/main/java/tech/techparterns/lead/application/usecase/ImportarLeadsUseCase.java` e excluído em `src/main/java/tech/techparterns/lead/application/usecase/ExcluirLeadUseCase.java`. O vendedor é `SELLER` em `app_user`. O e-mail de senha usa `EmailParaEnviar` e a tabela `envio_email`, cujo `tipo` só admite `SETUP` e `RESET`.

Fluxo desejado:

1. Lead novo nasce `ABERTA`, não quente, sem UF, sem cidade e sem dono.
2. Se a automática está ligada, a regra escolhida pode gravar dono. `FILA` não grava.
3. Admin lista sem dono, atribui, simula e confirma lote ou carteira.
4. Vendedor pega a fila ou devolve com motivo.
5. Troca de dono insere `assignment` e publica o evento. Simulação não grava.
6. Verificação de parado, quando chamada, aplica a RN-DS-10 e, se devolver à fila, envia e-mail depois do commit.

Camadas: banco (migração Flyway nova), backend (lead e distribuicao), integração de e-mail (reuso de `EnviadorEmail` e `envio_email`). Sem tarefa de frontend.

Escolhas técnicas deste plano, não decisões de produto:

- Fatos do lead nas colunas novas da tabela `lead`.
- `company_id` em `distribution_settings` e `territory`.
- Ponteiro como índice da última vaga confirmada; empate de `created_at` do vendedor por `app_user.id`; no peso, cada elegível ocupa `peso` vagas por ciclo.
- Empate de criação do lead na fila por `lead.id`.
- Fila com `SELECT … FOR UPDATE SKIP LOCKED` na linha de dono.
- Evento CDI `LeadAtribuido` com empresa, lead, dono anterior, dono novo, regra e id da linha, disparado na transação e observado após o commit.
- E-mail de parado em `envio_email` com `tipo` `PARADO` (cabe em `varchar(10)`). Não reutiliza o record `EmailParaEnviar`, porque ele carrega `TipoTokenSenha` e token de senha.
- Autorização dos recursos novos com `@RolesAllowed("ADMIN")` ou `@RolesAllowed("SELLER")`, como em `UsuarioResource` e `PerfilResource`. Empresa pelo `TenantContext`, sem `companyId` no command.

## 4. Tarefas

### Tarefa 1 — Schema

- **Objetivo:** criar as colunas e tabelas que a spec nomeia, com recorte por empresa.
- **Arquivos envolvidos:** `src/main/resources/db/migration/V5__criar_distribuicao.sql` (arquivo novo proposto). Migrações `V1` a `V4` em `src/main/resources/db/migration/` são confirmadas e não são editadas. A `V5` altera o `CHECK` de `envio_email.tipo` para incluir `PARADO`.
- **Dependências:** nenhuma.
- **Critérios de conclusão:** a migração sobe no PostgreSQL de teste. `lead` tem situação com padrão `ABERTA`, UF, cidade e quente com padrão falso. Existem `lead_ownership`, `assignment`, `seller_distribution_config`, `territory` e `distribution_settings`. O par UF/cidade de território é único por empresa. `lead_ownership.lead_id` é único.

### Tarefa 2 — Fatos no lead

- **Objetivo:** o agregado `Lead` passa a carregar situação, UF, cidade e quente, com os padrões da RN-DS-13 na criação.
- **Arquivos envolvidos:** confirmados — `src/main/java/tech/techparterns/lead/domain/model/Lead.java`, `src/main/java/tech/techparterns/lead/infrastructure/persistence/LeadEntity.java`, `src/main/java/tech/techparterns/lead/infrastructure/persistence/mapper/LeadMapper.java`, `src/main/java/tech/techparterns/lead/domain/repository/LeadRepository.java`, `src/main/java/tech/techparterns/lead/infrastructure/persistence/LeadRepositoryImpl.java`. Propostos — `src/main/java/tech/techparterns/lead/domain/model/SituacaoLead.java` e o teste `src/test/java/tech/techparterns/lead/domain/model/LeadDistribuicaoTest.java`.
- **Dependências:** tarefa 1.
- **Critérios de conclusão:** `Lead.tryCreate` grava `ABERTA`, quente falso e UF/cidade vazias. Lead já persistido sem as colunas antigas não existe após a migração, porque a migração preenche o padrão. Reconstituição devolve os quatro fatos. Teste de `LeadTest` existente continua passando.

### Tarefa 3 — Modelo de distribuição

- **Objetivo:** representar dono, histórico, configuração, território e settings sem framework.
- **Arquivos envolvidos:** propostos em `src/main/java/tech/techparterns/distribuicao/domain/model/`. Nenhum desses arquivos existe hoje.
- **Dependências:** nenhuma.
- **Critérios de conclusão:** um lead não aceita dois donos no modelo. Motivo de devolução vazio ou acima de 200 caracteres falha. Peso fora de 0 a 10 falha. `dias_para_parado` menor que 1 falha. Limite ausente e limite zero são válidos. Teste de unidade cobre essas falhas.

### Tarefa 4 — Persistência da distribuição

- **Objetivo:** ler e gravar as tabelas novas por `CompanyId`.
- **Arquivos envolvidos:** propostos em `src/main/java/tech/techparterns/distribuicao/infrastructure/persistence/` e `domain/repository/`. Teste proposto ao lado de `src/test/java/tech/techparterns/lead/infrastructure/persistence/LeadRepositoryTest.java`, no pacote `distribuicao`.
- **Dependências:** tarefas 1 e 3.
- **Critérios de conclusão:** salvar e reler dono nulo, linha de `assignment` e settings de uma empresa não aparece na consulta de outra empresa.

### Tarefa 5 — Portas para lead, vendedor e e-mail

- **Objetivo:** a distribuição não importa repositório de lead nem de identidade. Cada dado de outro módulo entra por porta no consumidor, com implementação no módulo dono, como a skill `.claude/skills/use-case/SKILL.md` exige.
- **Arquivos envolvidos:** propostos — portas em `src/main/java/tech/techparterns/distribuicao/domain/port/`. Implementação de leitura e atualização dos fatos do lead no pacote `lead`. Implementação de leitura do vendedor (`papel`, `status`, `created_at`, `email`, empresa) no pacote `identidade`, a partir de `UsuarioEmpresaEntity` confirmado. A porta de e-mail fica só declarada aqui; a implementação é a tarefa 16.
- **Dependências:** tarefas 2 e 4.
- **Critérios de conclusão:** um teste de repositório obtém, para um lead importado, situação `ABERTA` e data de criação, e obtém um `SELLER` `ACTIVE` com `created_at`.

### Tarefa 6 — Regras puras

- **Objetivo:** escolher destino e ordem da fila sem banco.
- **Arquivos envolvidos:** proposto — `src/main/java/tech/techparterns/distribuicao/domain/service/MotorDistribuicao.java` e teste de unidade no mesmo pacote de teste.
- **Dependências:** tarefa 3.
- **Critérios de conclusão:** o teste observa CA-08, CA-09, CA-10 e CA-11, a ordem da RN-DS-08 usada no CA-15, peso zero recebendo no rodízio simples, cidade com espaço nas pontas e UF em minúsculas casando como a RN-DS-19, e cidade de acento diferente não casando.

### Tarefa 7 — Troca de dono

- **Objetivo:** gravar no máximo uma troca por lead, com linha e evento, ou não gravar nada se o dono não mudou.
- **Arquivos envolvidos:** propostos — serviço de aplicação em `src/main/java/tech/techparterns/distribuicao/application/` e o record `LeadAtribuido` no domínio. A carga é a escolha técnica da seção 3 deste plano.
- **Dependências:** tarefas 4, 5 e 6.
- **Critérios de conclusão:** atribuir o mesmo dono duas vezes deixa uma linha. A segunda troca substitui o dono e insere outra linha. O evento é observado depois do commit. Simulação não chama este serviço.

### Tarefa 8 — Listar sem dono

- **Objetivo:** o admin vê só o conjunto da RF-DS-01, com os quatro filtros combinados.
- **Arquivos envolvidos:** confirmado — `src/main/java/tech/techparterns/lead/infrastructure/rest/LeadResource.java` não recebe esta rota. Propostos — `src/main/java/tech/techparterns/distribuicao/infrastructure/rest/DistribuicaoResource.java` em `@Path("/distribuicao")` e o caso de uso de listagem. Sem `page` nem `perPage`.
- **Dependências:** tarefa 5.
- **Critérios de conclusão:** CA-05 passa. `SELLER` recebe 403. Lead de outra empresa não aparece.

### Tarefa 9 — Atribuição manual

- **Objetivo:** aplicar a RF-DS-02.
- **Arquivos envolvidos:** métodos novos propostos em `DistribuicaoResource` e o caso de uso. Papel `ADMIN`.
- **Dependências:** tarefa 7.
- **Critérios de conclusão:** CA-06 passa. Vendedor destino inválido não grava nenhuma linha do pedido. No mesmo pedido, lead `ABERTA` muda e lead `GANHO` não muda.

### Tarefa 10 — Simular e confirmar lote

- **Objetivo:** simular sem efeito e confirmar executando a regra de novo sobre o conjunto da RF-DS-03.
- **Arquivos envolvidos:** casos de uso propostos, chamados pelo `DistribuicaoResource`.
- **Dependências:** tarefas 6 e 7.
- **Critérios de conclusão:** CA-12 e CA-13 passam. A simulação não muda ponteiro. Filtro de UF deixa lead de outra UF sem dono e sem linha nova.

### Tarefa 11 — Redistribuir carteira

- **Objetivo:** repetir simulação e confirmação sobre o conjunto da RF-DS-08, não sobre o filtro da lista sem dono.
- **Arquivos envolvidos:** casos de uso propostos no mesmo resource.
- **Dependências:** tarefa 10.
- **Critérios de conclusão:** CA-14 passa. Lead `GANHO` do vendedor continua com ele depois da confirmação.

### Tarefa 12 — Fila e devolução

- **Objetivo:** o vendedor pega o próximo lead ou devolve o próprio com motivo.
- **Arquivos envolvidos:** casos de uso propostos. A consulta da fila usa `SKIP LOCKED`, escolha técnica da seção 3.
- **Dependências:** tarefas 6 e 7.
- **Critérios de conclusão:** CA-07, CA-15 e CA-16 passam. Dois pedidos concorrentes não ficam com o mesmo lead. `ADMIN` nessas rotas recebe 403.

### Tarefa 13 — Editar fatos do lead

- **Objetivo:** admin grava situação, quente, UF e cidade; vendedor dono grava situação e quente.
- **Arquivos envolvidos:** confirmado — `LeadResource.java`, com método novo proposto. Caso de uso proposto na distribuição, usando a porta da tarefa 5. A transição que não muda dono não chama a tarefa 7.
- **Dependências:** tarefa 5.
- **Critérios de conclusão:** CA-17 passa. `GANHO` não volta a `ABERTA`. UF de um caractere não grava.

### Tarefa 14 — Configuração e território

- **Objetivo:** gravar peso, limite, participação, território e a linha de settings por empresa.
- **Arquivos envolvidos:** casos de uso propostos no `DistribuicaoResource`. Settings ausentes se comportam como automática desligada, peso 1, sem limite e participação ligada.
- **Dependências:** tarefa 4.
- **Critérios de conclusão:** CA-22 passa pelo HTTP 409 já mapeado em `src/main/java/tech/techparterns/shared/infrastructure/rest/GlobalExceptionHandler.java` para SQLState `23505`. Peso 11 não grava. `dias_para_parado` 0 não grava. Vendedor de outra empresa não vira território.

### Tarefa 15 — Histórico

- **Objetivo:** o admin lê as linhas do lead da empresa, da mais antiga para a mais nova.
- **Arquivos envolvidos:** caso de uso proposto no `DistribuicaoResource`.
- **Dependências:** tarefa 7.
- **Critérios de conclusão:** depois de duas trocas, a resposta vem na ordem em que foram gravadas. `SELLER` recebe 403. Exclusão lógica posterior não remove as linhas.

### Tarefa 16 — E-mail do parado e verificação invocável

- **Objetivo:** quando a verificação é chamada, aplicar a RN-DS-10. Com devolução ligada, gravar a troca antes do e-mail. Falha definitiva do envio não desfaz a troca e fica em `envio_email`.
- **Arquivos envolvidos:** confirmados — `src/main/java/tech/techparterns/identidade/domain/port/EnviadorEmail.java`, `src/main/java/tech/techparterns/identidade/infrastructure/mail/EnvioEmailListener.java` não é alterado para token de senha. Propostos — evento CDI próprio do aviso, texto provisório em classe nova de infraestrutura, e alteração só do `CHECK` já prevista na tarefa 1. O texto provisório diz que o lead voltou à fila e inclui o nome do contato. Ele não é decisão de produto aprovada.
- **Dependências:** tarefas 1, 7 e 14.
- **Critérios de conclusão:** CA-18 e CA-19 passam. Não há dependência nova de scheduler no `pom.xml`. Nenhuma tarefa agenda a verificação.

### Tarefa 17 — Automática na importação

- **Objetivo:** só o ramo que cria lead em `ImportarLeadsUseCase` chama a distribuição. Completar campos e ignorar duplicata não chamam.
- **Arquivos envolvidos:** confirmado — `src/main/java/tech/techparterns/lead/application/usecase/ImportarLeadsUseCase.java`. Proposta — porta `DistribuirLeadNovo` no pacote `lead`, implementada no pacote `distribuicao`.
- **Dependências:** tarefas 7 e 14.
- **Critérios de conclusão:** CA-02, CA-03 e CA-04 passam. O teste existente `src/test/java/tech/techparterns/lead/infrastructure/rest/LeadResourceTest.java` continua passando com a automática desligada.

### Tarefa 18 — Regressão de exclusão e desativação

- **Objetivo:** provar que exclusão e desativação não movem dono.
- **Arquivos envolvidos:** confirmados — `ExcluirLeadUseCase.java` e `src/main/java/tech/techparterns/identidade/application/usecase/DesativarUsuarioUseCase.java`. Nenhum dos dois ganha chamada de distribuição. Testes propostos nos pacotes de teste já existentes desses módulos.
- **Dependências:** tarefas 7 e 15.
- **Critérios de conclusão:** CA-20 passa. Depois de desativar o vendedor, o lead `ABERTA` segue com o mesmo dono até a confirmação da tarefa 11.

## 6. Estratégia de testes

Testes de unidade, sem banco: modelo da tarefa 3 e `MotorDistribuicao` da tarefa 6. Cobrem proporção, ponteiro, território, fila e validação de motivo, peso e prazo.

Testes de integração, com PostgreSQL de teste já configurado em `src/main/resources/application.properties` (`%test.quarkus.datasource.devservices.image-name`): repositórios, recursos REST com token de `ADMIN` e `SELLER`, importação, concorrência da fila e o 409 do território. O padrão de recurso existente está em `LeadResourceTest.java`.

Testes de interface: não se aplicam. Não há frontend neste repositório e a validação não verificou tela.

## 7. Riscos e medidas de mitigação

- **Demonstrado:** `ImportarLeadsUseCase` compara a constante `"admin"` com o literal `"admin"` e não lê o papel do token. A tarefa 17 não corrige isso, porque a spec não pede essa correção. Os recursos novos não usam essa comparação; usam `@RolesAllowed`.
- **Demonstrado:** `EmailParaEnviar` exige `TipoTokenSenha` e token de senha. A tarefa 16 não coloca o aviso de parado nesse record, para não mudar o e-mail de senha. O registro continua em `envio_email`.
- **Demonstrado:** `pom.xml` não tem `quarkus-scheduler`. O plano não adiciona a dependência. A verificação só roda quando o caso de uso é chamado.
- **Hipotético:** a lista sem paginação pode ficar grande. Não há volume aprovado. A mitigação é não inventar página nesta feature. Paginar continua fora até aprovação humana.
- **Hipotético:** `SKIP LOCKED` pode não ser o mecanismo desejado em outro banco. O datasource confirmado é PostgreSQL. A mitigação é o CA-15 observar o resultado, não o SQL.

Decisões que continuam sem aprovação humana e não são tarefa:

- Substituir o texto provisório do e-mail.
- Escolher um intervalo e passar a agendar a verificação.
- Paginar a lista sem dono.
- Trocar a carga de `LeadAtribuido` adotada na seção 3.

## 8. Critérios de conclusão da feature

A feature estará pronta quando os CA-01 a CA-22 da spec forem observados nos testes das tarefas, e quando o teste da tarefa 6 observar a RN-DS-19. Também precisa ser verdadeiro:

- automática desligada não atribui lead importado;
- simulação não grava dono, histórico, ponteiro nem e-mail;
- falha definitiva do e-mail de parado não devolve o lead ao dono anterior e deixa registro em `envio_email`;
- empresa do token não lê dado de outra empresa;
- desativar vendedor não muda dono;
- excluir lead não cria nem apaga linha de histórico.

## 9. Checklist final de implementação

- [ ] Migração `V5` aplicada e reversível apenas por migração nova, sem editar `V1` a `V4`.
- [ ] Lead novo nasce `ABERTA`, não quente, sem UF e sem cidade.
- [ ] Dono único, histórico só em troca, evento publicado após o commit.
- [ ] Lote geral respeita filtro e confirma executando a regra de novo.
- [ ] Carteira redistribuída é só a `ABERTA` do vendedor.
- [ ] Fila e devolução restritas ao `SELLER` elegível.
- [ ] Manual aceita vendedor `ACTIVE` mesmo no limite ou fora da participação.
- [ ] Território duplicado na empresa responde 409 e não apaga o primeiro.
- [ ] Verificação de parado é invocável e não está agendada.
- [ ] E-mail provisório não é apresentado como texto aprovado.
- [ ] `ImportarLeadsUseCase` só dispara a automática na criação.
- [ ] `ExcluirLeadUseCase` e `DesativarUsuarioUseCase` não ganham chamada de distribuição.
- [ ] Não há tarefa de tela nem dependência de scheduler.
- [ ] Nenhuma destas caixas foi executada por este plano.
