# Especificação — Distribuição

Entrada: `docs/features/distribuicao/discovery.md`, aprovado em 16 de outubro de 2026. Esta especificação não está validada.

As decisões aprovadas são requisito. Alternativas não escolhidas e os critérios preliminares do discovery não entram como requisito. Onde o discovery não fechou um comportamento, a pergunta fica na seção 10 e o texto não escolhe a resposta.

## 1. Contexto e problema

O CRM precisa de uma única fonte de verdade para o dono de cada lead e de um histórico de cada troca de dono. Hoje o lead não tem dono.

O lead persistido em `src/main/resources/db/migration/V1__criar_tabela_lead.sql` e modelado em `src/main/java/tech/techparterns/lead/domain/model/Lead.java` guarda empresa, nome, telefone, WhatsApp, e-mail, origem e exclusão lógica. Não guarda dono, situação comercial, UF, cidade, marca de quente nem data de atribuição.

A criação de lead ocorre em `src/main/java/tech/techparterns/lead/application/usecase/ImportarLeadsUseCase.java`. O CSV, em `src/main/java/tech/techparterns/lead/application/csv/CsvLeads.java`, traz `nome_contato`, `telefone`, `whatsapp` e `email`. A origem gravada é `IMPORTACAO` (`src/main/java/tech/techparterns/lead/domain/model/OrigemLead.java`). Não há outro ponto de criação de lead neste repositório.

O vendedor existe como `SELLER` em `app_user`, com status `PENDING`, `ACTIVE` ou `INACTIVE` (`src/main/resources/db/migration/V2__criar_tabelas_empresa_e_usuarios.sql`). A desativação em `src/main/java/tech/techparterns/identidade/application/usecase/DesativarUsuarioUseCase.java` não escreve na tabela `lead`.

Não existem, neste repositório, funil, território, fila de distribuição, histórico de dono nem o evento `LeadAtribuido`.

## 2. Objetivos

- Registrar no máximo um dono por lead e a fila quando não houver dono.
- Guardar cada troca de dono e publicar `LeadAtribuido` nessa troca.
- Permitir ao admin listar leads sem dono, atribuir seleção, distribuir em lote com simulação, configurar vendedor e território, ligar a distribuição automática e redistribuir a carteira aberta de um vendedor.
- Permitir ao vendedor pegar o próximo lead da fila e devolver um lead seu com motivo.
- Aplicar rodízio, peso, carga, território, limite de leads abertos e lead parado como o discovery aprovou.

## 3. Escopo e fora de escopo

### Escopo

- Dono, histórico, configuração do vendedor, território, regras, ponteiro de rodízio e a linha de configuração por empresa.
- No lead: situação `ABERTA`, `GANHO` ou `PERDIDO`; UF e cidade opcionais; `quente`; parado contado desde a última atribuição de dono.
- Atores admin e vendedor da empresa do token.
- E-mail ao dono anterior quando o lead parado volta à fila.

### Fora de escopo

- Funil com várias etapas, score de temperatura e feed de atividades.
- Reabrir `GANHO` ou `PERDIDO`.
- Mover leads na desativação do vendedor, sem uma redistribuição confirmada pelo admin.
- Apagar histórico na exclusão lógica do lead.
- Texto do e-mail, contrato dos consumidores de `LeadAtribuido` e qualquer tela Angular. O frontend não está neste repositório.

## 4. Requisitos funcionais

Os identificadores abaixo são os do discovery aprovado.

- **RF-DS-01.** O admin vê somente leads `ABERTA`, sem dono e sem exclusão lógica, da própria empresa. Os filtros informados valem juntos: UF, cidade, quente e origem. Filtro omitido não restringe esse campo.
- **RF-DS-02.** O admin atribui a um vendedor `ACTIVE` da mesma empresa os leads selecionados que estão sem dono, `ABERTA` e sem exclusão. Lead `GANHO`, `PERDIDO` ou excluído não muda de dono. Lead que já tem outro dono fica na seção 10.
- **RF-DS-03.** O admin distribui um lote pela regra escolhida: rodízio, rodízio com peso, carga ou território. O conjunto de entrada desse lote geral está em aberto na seção 10. Para cada lead do conjunto, a regra e o resultado parcial estão especificados.
- **RF-DS-04.** Antes de confirmar, o admin vê quantos leads cada vendedor receberia e quantos continuariam sem dono. Essa consulta não grava dono, histórico, evento nem ponteiro.
- **RF-DS-05.** O admin configura, por vendedor da empresa, peso de 0 a 10, limite de leads abertos e participação na distribuição. Ausência de configuração equivale a peso 1, sem limite e participação ligada.
- **RF-DS-06.** O admin configura territórios da empresa: UF obrigatória e cidade opcional. Cidade ausente cobre a UF inteira. O par UF e cidade é único na empresa. Cada território aponta para um vendedor da empresa.
- **RF-DS-07.** O admin liga ou desliga a distribuição automática de lead novo e, ao ligar, informa a regra: `RODIZIO`, `PESO`, `CARGA`, `TERRITORIO` ou `FILA`. O padrão, sem linha gravada, é automática desligada.
- **RF-DS-08.** O admin redistribui a carteira de um vendedor pelo mesmo lote, com simulação e confirmação. O conjunto é o dos leads `ABERTA`, não excluídos, de que ele é dono. `GANHO`, `PERDIDO` e excluído ficam de fora.
- **RF-DS-09.** O vendedor pega o próximo lead da fila da própria empresa quando está `ACTIVE`, participa da distribuição e está abaixo do limite.
- **RF-DS-10.** O vendedor devolve à fila um lead `ABERTA` de que é dono, com motivo. O motivo é obrigatório e tem no máximo 200 caracteres.
- **RF-DS-11.** O admin vê o histórico de donos de um lead da própria empresa. A exclusão lógica do lead não apaga essas linhas.

Quem grava os fatos do lead:

- O admin grava situação, quente, UF e cidade de um lead da empresa, esteja o lead na fila ou com dono.
- O vendedor grava situação e quente só do lead de que é dono. Não grava UF nem cidade.
- Lead na fila: só o admin grava esses campos.
- `GANHO` e `PERDIDO` não voltam para `ABERTA`. A situação definitiva não altera o dono.
- Alterar UF, cidade, quente ou situação não gera `assignment` nem `LeadAtribuido`, porque o dono não muda.

## 5. Regras de negócio

- **RN-DS-01.** Um lead tem no máximo um dono. Dono ausente significa fila.
- **RN-DS-02.** Regra automática, lote, território, carga, peso, rodízio e fila só entregam lead a vendedor `ACTIVE` da empresa com participação ligada. Atribuição manual aceita qualquer vendedor `ACTIVE` da empresa, com ou sem participação e com ou sem limite atingido.
- **RN-DS-03.** O rodízio percorre os vendedores elegíveis na ordem de cadastro (`app_user.created_at`, da mais antiga para a mais nova) e continua do ponteiro salvo da empresa. Ponteiro vazio começa no primeiro elegível dessa ordem. Vendedor inelegível é pulado. O ponteiro avança quando uma confirmação, ou a distribuição automática, atribui por rodízio ou por rodízio com peso, e quando a carga desempata por rodízio.
- **RN-DS-04.** No rodízio com peso, ao longo de um ciclo completo entre os elegíveis, peso 2 recebe o dobro de peso 1. Peso 0 não recebe por essa regra. Peso 0 não impede o mesmo vendedor de receber por outra regra ou por atribuição manual.
- **RN-DS-05.** Por carga, o lead vai para o elegível com menos leads `ABERTA` não excluídos. Empate usa o rodízio entre os empatados e avança o ponteiro. Sem empate, o ponteiro não muda. `GANHO` e `PERDIDO` não entram na contagem.
- **RN-DS-06.** Por território, a cidade do lead tem prioridade sobre a UF inteira. O vendedor da cidade, se elegível, recebe com regra `TERRITORIO`. Se esse vendedor existe e não é elegível, o lead cai no rodízio e não passa ao vendedor da UF. Sem território de cidade, vale o território da UF inteira, se o vendedor for elegível. Sem par correspondente, cai no rodízio.
- **RN-DS-07.** Vendedor cujo número de leads `ABERTA` não excluídos já é igual ao limite é pulado nas regras e não pega da fila. Limite ausente não pula.
- **RN-DS-08.** Dois vendedores não ficam donos do mesmo lead. A fila, entre leads `ABERTA`, sem dono e não excluídos, entrega nesta ordem: quentes, do criado há mais tempo ao mais novo, mesmo que tenham sido devolvidos ou tenham voltado por parado; depois os não quentes que não foram devolvidos nem voltaram por parado, do criado há mais tempo ao mais novo; por último os não quentes devolvidos ou que voltaram por parado, do criado há mais tempo ao mais novo. «Devolvido» é a última ida à fila com regra `DEVOLUCAO`. «Voltou por parado» é a última ida à fila com regra `PARADO`. Uma ida à fila por lote sem destino não entra nesses dois grupos.
- **RN-DS-09.** Lead `GANHO` ou `PERDIDO` não muda de dono por manual, lote, automática, fila, devolução, parado ou redistribuição.
- **RN-DS-10.** Lead `ABERTA`, não excluído e com dono, cuja última atribuição de dono foi há mais de N dias, fica marcado como parado. N vem da configuração da empresa; o padrão é 5. Com a devolução desligada, o dono permanece, não há `assignment`, não há `LeadAtribuido` e não há e-mail. Com a devolução ligada, o lead volta à fila, gera `assignment` com regra `PARADO`, publica `LeadAtribuido` e envia e-mail ao vendedor que era o dono, no e-mail cadastrado em `app_user.email`. A falha desse envio não desfaz a volta à fila nem apaga o histórico. Lead sem dono não é marcado por essa regra. Nova atribuição de dono zera a marca de parado e passa a contar o prazo desde essa atribuição.
- **RN-DS-11.** Desativar um vendedor não muda dono. Os leads `ABERTA` dele seguem disponíveis para o admin redistribuir pela RF-DS-08.
- **RN-DS-12.** Toda troca de dono insere uma linha de histórico e publica `LeadAtribuido`. Ausência de troca não insere linha e não publica o evento. A linha guarda lead, dono anterior, dono novo, regra, motivo, autor e instante. Autor vazio significa automático. Motivo é obrigatório só em `DEVOLUCAO`.

Regras derivadas das decisões, necessárias para as anteriores serem executáveis:

- **RN-DS-13.** Lead novo nasce `ABERTA`, não quente, sem UF, sem cidade e sem dono. Não nasce parado.
- **RN-DS-14.** Lead excluído logicamente não entra na fila, no lote, na carga, na redistribuição nem na automática. A exclusão não gera `assignment` nem `LeadAtribuido`.
- **RN-DS-15.** No lote e na automática, lead sem vendedor que possa receber permanece ou volta à fila. Se o dono muda para ausente, a troca é gravada com a regra da operação e dono novo vazio. Se já estava sem dono, não há linha.
- **RN-DS-16.** A regra `FILA`, na automática, não escolhe vendedor. O lead novo permanece sem dono e sem linha de histórico.
- **RN-DS-17.** Simulação e confirmação são operações distintas. A confirmação executa a regra de novo sobre o conjunto e só então grava. A simulação não reserva lead.
- **RN-DS-18.** Configuração, ponteiro, território, dono e histórico de uma empresa não são lidos nem gravados por outra empresa.

Valores de regra no histórico: `MANUAL`, `RODIZIO`, `PESO`, `CARGA`, `TERRITORIO`, `FILA`, `DEVOLUCAO`, `PARADO`.

## 6. Requisitos não funcionais relevantes

- Toda leitura e escrita usa a empresa do token. O token de empresa já expõe `companyId` em `src/main/java/tech/techparterns/shared/infrastructure/security/TenantContext.java`.
- O histórico de dono é somente inserção. Linha já gravada não é apagada por exclusão de lead, falha de e-mail ou desativação de vendedor.
- A simulação não tem efeito colateral observável em dono, histórico, evento, ponteiro ou e-mail.
- A volta do lead parado à fila permanece confirmada ainda que o e-mail falhe. A falha fica registrada à parte.
- Dois vendedores que pedem a fila ao mesmo tempo não recebem o mesmo lead. O mecanismo de concorrência é proposta técnica, não requisito.

Não há meta de tempo, volume ou disponibilidade aprovada. Essa seção não inventa uma.

## 7. Fluxos e cenários de exceção

### Listar sem dono

O admin consulta a fila qualificada da RF-DS-01. Vendedor que consulta recebe recusa e não vê leads de outra carteira. Lead de outra empresa não aparece.

### Atribuir seleção

O admin informa os leads e um vendedor `ACTIVE` da empresa. Cada lead sem dono, `ABERTA` e não excluído passa a ter esse dono, com regra `MANUAL`, autor admin e dono anterior vazio. Lead `GANHO`, `PERDIDO` ou excluído não muda de dono. O lead que já tem outro dono está na seção 10.

Recusa a operação inteira, sem gravar troca, quando o vendedor não existe na empresa, não é `SELLER` ou não está `ACTIVE`.

### Simular e confirmar lote

A simulação devolve a contagem por vendedor e a contagem que continuaria sem dono. Não grava.

A confirmação aplica a regra. Cada lead com destino muda de dono, gera linha com a regra escolhida e publica `LeadAtribuido`. O autor é o admin no lote manualmente confirmado e fica vazio na automática. Lead sem destino e que já estava sem dono não gera linha. Lead com dono que fica sem destino gera linha com dono novo vazio.

### Redistribuir carteira

O conjunto é o da RF-DS-08. O restante do fluxo é o lote. Desativar o vendedor, por `DesativarUsuarioUseCase`, não dispara esse fluxo.

### Distribuição automática

Quando a automática está ligada, cada lead novo persistido por `ImportarLeadsUseCase` no ramo de criação passa pela regra escolhida. Completar campos vazios de um lead já existente e ignorar duplicata não disparam a regra. Com a automática desligada, o lead novo fica sem dono e sem linha.

### Pegar da fila

O vendedor elegível recebe o primeiro lead da ordem da RN-DS-08. A linha usa regra `FILA`, dono anterior vazio e autor igual ao vendedor. Fila vazia não grava linha e informa que não há lead. Vendedor no limite, inativo, pendente, fora da participação ou que não é `SELLER` não recebe lead.

### Devolver

O vendedor informa o lead e o motivo. O lead `ABERTA` de que ele é dono volta à fila, com regra `DEVOLUCAO`, motivo, autor vendedor e dono novo vazio. Motivo vazio ou acima de 200 caracteres não muda o dono. Lead de outro dono, de outra empresa, excluído, `GANHO` ou `PERDIDO` não muda de dono.

### Marcar parado

Uma verificação posterior ao prazo marca o lead da RN-DS-10. O instante em que a verificação roda não foi aprovado; a regra do que ela faz está aprovada. Com devolução ligada, o e-mail sai depois da troca gravada. Falha de envio deixa a troca como está.

### Editar fatos do lead

Transição permitida: `ABERTA` para `GANHO` ou `PERDIDO`, pelo admin, ou pelo vendedor dono. Transição a partir de `GANHO` ou `PERDIDO` não grava. Vendedor que não é o dono não grava situação nem quente. Qualquer um que não seja admin não grava UF nem cidade. Admin grava UF com dois caracteres e cidade com até 80, ou limpa os dois campos.

### Território e configuração

Segundo território com o mesmo par UF e cidade na mesma empresa não substitui o primeiro. O conflito de unicidade já existente no PostgreSQL é mapeado para HTTP 409 e código `CONFLITO` em `src/main/java/tech/techparterns/shared/infrastructure/rest/GlobalExceptionHandler.java`. Peso fora de 0 a 10 não grava. Vendedor de outra empresa não entra em território nem em configuração.

### Histórico

O admin recebe as linhas do lead da empresa, da mais antiga para a mais nova. Vendedor não recebe esse histórico. Lead de outra empresa não devolve linhas.

### Exclusão

`ExcluirLeadUseCase` continua podendo excluir o lead. A exclusão não cria dono, não remove dono por meio de `assignment` e não publica `LeadAtribuido`. As linhas antigas continuam consultáveis pelo admin.

## 8. Impactos técnicos identificados

### Comportamento atual

- O lead não tem dono, situação, UF, cidade nem quente. Tabela e entidade citadas na seção 1.
- A única criação de lead é a importação. O CSV não traz UF nem cidade, então um lead importado nasce sem território até o admin gravar esses fatos.
- `ImportarLeadsUseCase` compara a constante `PAPEL_ADMIN` (`"admin"`) com o literal `"admin"` e não lê `TenantContext.role()`. `ExcluirLeadUseCase` compara `tenant.role()` com `"admin"`. O JWT de empresa coloca em `groups` o nome do enum `PapelAcesso` (`ADMIN`, `SELLER`), em `src/main/java/tech/techparterns/identidade/infrastructure/security/EmissorAccessTokenJwt.java`. `LeadResource` não declara `@RolesAllowed`.
- `DesativarUsuarioUseCase` não altera lead.
- O corpo de erro padrão é `ErroResponse` (`path`, `mensagem`, `codigoErro`, `dataHora`). `DomainException` vira HTTP 400 e `REQUISICAO_INVALIDA`. `RecursoNaoEncontradoException` vira HTTP 404 e `RECURSO_NAO_ENCONTRADO`. Papel insuficiente nas rotas que usam `@RolesAllowed` vira HTTP 403 e `ACESSO_NEGADO`.
- E-mail existente: `EnviadorEmail`, `EnvioEmailListener` (depois do commit, com novas tentativas) e a tabela `envio_email`. O `CHECK` de `tipo` admite só `SETUP` e `RESET` (`V4__criar_tabelas_auditoria_envio_e_login.sql`). O listener não guarda corpo nem assunto.
- Não há `quarkus-scheduler` no `pom.xml`. As migrações Flyway atuais vão de `V1` a `V4`. O banco configurado é PostgreSQL.
- O único evento de aplicação encontrado no envio de e-mail é o CDI `EmailParaEnviar`, observado em `TransactionPhase.AFTER_SUCCESS`. Não existe `LeadAtribuido`.
- Não há código Angular neste repositório. Há URL de frontend e CORS para `http://localhost:4200` em `src/main/resources/application.properties`. O impacto de tela não foi verificado.
- As convenções de módulo estão em `.claude/skills/` (entity, value-object, repository, use-case, dto, domain-service, query-cqrs, resource). O núcleo compartilhado está em `tech.techparterns.shared`.

### Comportamento desejado

O desejado é o das seções 4 a 7. Em particular: dono e histórico por empresa; fatos mínimos no lead; regras e fila na ordem aprovada; simulação sem efeito; e-mail só na volta do parado à fila; desativação sem mover lead.

### Decisões técnicas propostas

Não são requisito. A seção 9 não depende delas, salvo onde o critério diz o contrário.

- Persistir situação, UF, cidade e quente na tabela `lead` já existente, para não criar outra fonte desses fatos. Nomes físicos de coluna ficam para o plano.
- Criar as tabelas aprovadas no discovery (`lead_ownership`, `assignment`, `seller_distribution_config`, `territory`, `distribution_settings`) numa migração Flyway posterior a `V4`. Incluir `company_id` em `distribution_settings` como chave da linha única por empresa, e em `territory` para garantir o par único por empresa. Ligar `lead_ownership.lead_id` e `assignment.lead_id` ao lead. Isso realiza o recorte por empresa; o discovery não nomeou a coluna `company_id`.
- Representar o ponteiro como a posição já consumida na ordem de cadastro. Empate de `created_at` desempata por `app_user.id`. No peso, cada elegível com peso maior que zero ocupa, por ciclo, tantas vagas quantas o peso, na ordem de cadastro. O ponteiro lembra a última vaga confirmada.
- Na fila, empate de data de criação desempata por `lead.id`.
- Exclusão mútua na fila por bloqueio da linha de dono com `SKIP LOCKED`, de forma que o segundo vendedor receba o lead seguinte ou a fila vazia. O requisito continua sendo só o resultado: ninguém fica com o mesmo lead.
- Publicar `LeadAtribuido` como evento CDI após a troca estar gravada, no mesmo estilo de `EmailParaEnviar`. Carga mínima proposta: empresa, lead, dono anterior, dono novo, regra e id da linha de histórico.
- Registrar o e-mail do parado no fluxo já existente de `envio_email`, estendendo o `CHECK` de `tipo`. O corpo não é persistido, como o listener atual já faz. O destinatário é `app_user.email`.
- Verificar o prazo de parado por uma varredura por empresa. O intervalo não está aprovado. Um teste pode invocar a verificação diretamente.
- Autorizar admin e vendedor pelos papéis `ADMIN` e `SELLER` do token, não pela comparação com a string minúscula `"admin"` usada hoje no módulo de lead.
- Seguir o recurso paginado de `.claude/skills/resource/references/resource-pattern.md` se a lista sem dono for paginada. Paginação não está aprovada.

## 9. Critérios de aceitação

Cada critério abaixo é observável a partir das decisões aprovadas.

- **CA-01.** Um lead não fica com dois donos. A segunda atribuição confirmada substitui a primeira e gera outra linha de histórico.
- **CA-02.** Lead novo importado, com automática desligada, fica sem dono, `ABERTA`, não quente, sem UF e sem cidade, e não gera linha de histórico.
- **CA-03.** Com automática ligada e regra de atribuição, o lead novo importado que tem destino ganha dono, uma linha e `LeadAtribuido`, com autor vazio. Completar campos de um lead existente não muda o dono.
- **CA-04.** Com regra `FILA` e automática ligada, o lead novo permanece sem dono e sem linha.
- **CA-05.** A lista do admin não contém lead com dono, `GANHO`, `PERDIDO` ou excluído. UF, cidade, quente e origem informados juntos restringem ao mesmo tempo.
- **CA-06.** Atribuição manual a vendedor `ACTIVE` grava dono e linha `MANUAL` para lead sem dono e `ABERTA`. O mesmo pedido para vendedor `PENDING` ou `INACTIVE` não grava nada.
- **CA-07.** Vendedor no limite ou com participação desligada não recebe por rodízio, peso, carga, território nem fila, e não pega da fila. A atribuição manual ainda pode escolhê-lo se estiver `ACTIVE`.
- **CA-08.** Três vendedores elegíveis, cadastrados em ordem A, B e C, sem limite, recebem quatro leads de rodízio na ordem A, B, C, A. A atribuição seguinte começa em B.
- **CA-09.** Num ciclo completo de peso, com um vendedor peso 2 e outro peso 1, ambos elegíveis, o primeiro recebe 2 leads e o segundo 1. Um terceiro com peso 0 não recebe por essa regra.
- **CA-10.** Por carga, o lead vai para quem tem menos leads `ABERTA`. Dois empatados resolvem pelo próximo do rodízio. Lead `GANHO` desse vendedor não entra na conta.
- **CA-11.** Lead com cidade coberta por um vendedor elegível vai para ele, e não para o vendedor da UF inteira. Se o vendedor da cidade está inativo, fora da distribuição ou no limite, o lead vai ao rodízio e não ao vendedor da UF.
- **CA-12.** A simulação de um lote não altera dono, histórico, ponteiro nem envia e-mail. A confirmação altera só os leads cujo dono mudou.
- **CA-13.** Num lote em que só parte dos leads tem destino, essa parte muda de dono e o restante sem dono continua sem linha nova.
- **CA-14.** Redistribuir a carteira não inclui lead `GANHO`, `PERDIDO` ou excluído. Desativar o vendedor deixa os leads `ABERTA` com ele até a confirmação dessa redistribuição.
- **CA-15.** Dois vendedores que pegam a fila ao mesmo tempo recebem leads diferentes, ou um deles recebe a informação de fila vazia. A ordem segue a RN-DS-08: quente na frente; não quente devolvido ou parado atrás de não quente que nunca voltou por esses motivos.
- **CA-16.** Devolução sem motivo, ou com motivo acima de 200 caracteres, não muda o dono. Devolução válida gera linha `DEVOLUCAO` e `LeadAtribuido`.
- **CA-17.** Vendedor que não é o dono não altera situação nem quente. Vendedor não altera UF nem cidade. Admin altera os quatro. `GANHO` e `PERDIDO` não voltam a `ABERTA`, e o dono permanece.
- **CA-18.** Lead `ABERTA` com o mesmo dono há mais de N dias fica marcado. Com a devolução desligada, o dono permanece e não há e-mail. Com a devolução ligada, o lead fica sem dono, há linha `PARADO`, há `LeadAtribuido` e o e-mail é endereçado ao `app_user.email` do dono anterior.
- **CA-19.** Se o envio do e-mail do parado falha, o lead continua sem dono e a linha `PARADO` continua consultável.
- **CA-20.** Excluir o lead não gera linha nova e não remove as linhas já gravadas. O lead excluído não volta à fila nem à carga.
- **CA-21.** Admin de outra empresa não lê nem grava dono, histórico, território, configuração ou fila da empresa do lead.
- **CA-22.** Segundo território com o mesmo par UF e cidade na mesma empresa não apaga o primeiro.

## 10. Dúvidas e decisões pendentes

- **Conjunto do lote geral.** O lote da RF-DS-03 percorre todos os leads sem dono da empresa, só uma seleção, ou só o resultado dos filtros? Bloqueia o contrato de entrada desse lote. Não bloqueia a regra aplicada a cada lead nem a redistribuição de carteira, cujo conjunto já é o da RF-DS-08.
- **Atribuição manual de lead que já tem dono.** O discovery fecha a atribuição de leads selecionados sem dono e fecha a redistribuição da carteira por lote. Não fecha a troca manual de um dono já existente. Bloqueia só esse caso.
- **Texto do e-mail e assunto.** Não foram aprovados. Não bloqueia destinatário, momento do envio nem a regra de falha.
- **Carga de `LeadAtribuido`.** A obrigação de publicar está aprovada. Os campos do evento não. Não bloqueia a publicação. Não há consumidor deste evento no repositório, então não bloqueia um contrato já existente.
- **Frontend Angular.** Não há código Angular neste repositório. O impacto de tela não foi verificado e não bloqueia o comportamento de backend desta especificação.
- **Marcar parado em lead `GANHO` ou `PERDIDO` sem tirar o dono.** A troca de dono desses leads está proibida. A simples marca, sem troca, não foi decidida. Não bloqueia a proibição de redistribuir.
- **Normalização de cidade e o valor inicial de `regra_padrao`.** A correspondência de território e a necessidade de uma regra escolhida ao ligar a automática estão decididas. Caixa, acento e o valor default da regra, enquanto a automática permanece desligada, não. Não bloqueiam os requisitos já escritos.
- **Limite negativo e prazo de parado menor que 1.** Não foram decididos. Não bloqueiam o padrão de 5 dias, o limite vazio e a faixa de peso de 0 a 10.
- **Paginação e ordenação da lista sem dono.** Não foram aprovadas. Não bloqueiam os filtros da RF-DS-01.
- **Algoritmo interno do peso e o bloqueio físico da fila.** A proporção, o ponteiro e a exclusão de dois donos estão decididos. As propostas da seção 8 não são requisito. Não bloqueiam os critérios CA-08, CA-09 e CA-15, que observam resultado e não a estrutura interna.
- **Simulação como snapshot.** Nenhuma decisão manda guardar a simulação. Esta especificação trata a confirmação como uma nova execução da regra. Não bloqueia a separação entre simular e confirmar.
