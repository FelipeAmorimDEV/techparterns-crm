# Distribuição

- **Nome:** Distribuição
- **Slug:** `distribuicao`
- **Estado:** aprovado
- **Aprovação:** o usuário respondeu «Aprovo» ao resumo do discovery em 16 de outubro de 2026. Esse resumo é o texto da seção Decisões, com as alternativas, os critérios preliminares e a ordem da fila registrados abaixo.

A Distribuição decide qual vendedor é dono de cada lead e guarda o histórico de toda troca de dono. É a única fonte de verdade do campo «dono».

O admin distribui, configura regras e territórios e redistribui carteiras. O vendedor pega o próximo lead da fila ou devolve um lead seu com motivo.

## Decisões

### Fronteira

A Distribuição é dona do dono, do histórico, da configuração do vendedor, do território do vendedor, das regras e do ponteiro de rodízio.

O lead ganha só os fatos que as regras precisam ler:

- Situação `ABERTA` (padrão na criação), `GANHO` ou `PERDIDO`. «Etapa aberta» significa `ABERTA`.
- UF e cidade opcionais.
- `quente`, booleano, padrão falso, marcado à mão.
- Parado contado desde `atribuido_em`: o mesmo dono segurou o lead por mais de N dias (padrão 5).

Ficam fora: funil com várias etapas, score de temperatura e feed de atividades (ligação, nota ou tarefa).

Configuração, ponteiro de rodízio, território e histórico são por empresa. «Linha única» em `distribution_settings` significa uma linha por empresa.

### Quem grava os fatos do lead

- O admin grava situação, quente, UF e cidade.
- O vendedor grava situação e quente somente do lead de que é dono.
- Com o lead na fila, só o admin grava esses campos.

`GANHO` e `PERDIDO` são definitivos. Ninguém reabre para `ABERTA`. O dono permanece. O lead sai da carga, da fila, do rodízio e da redistribuição. Uma marcação errada permanece.

### Território

O par UF e cidade é único por empresa. Cada território tem um vendedor. Cidade específica tem prioridade sobre a UF inteira.

Sem par correspondente, o lead cai no rodízio. Vendedor do território inativo, fora da distribuição ou no limite de leads abertos também conta como território sem correspondente: o lead cai no rodízio. O rodízio continua pulando quem não pode receber.

### Lote, automático e carteira

No lote, a confirmação atribui os leads que têm destino e deixa o restante na fila. A simulação mostra as duas quantidades antes de confirmar. Quem permanece na fila não gera linha em `assignment`.

A distribuição automática de lead novo segue a mesma regra: sem vendedor que possa receber, o lead fica sem dono.

Redistribuir a carteira usa esse mesmo lote. Os leads `ABERTA` daquele vendedor entram na regra escolhida, com simulação antes de confirmar. `GANHO`, `PERDIDO` e excluído ficam de fora. Ao desativar um vendedor, os leads abertos continuam com ele até o admin confirmar esse lote. Nada se move sozinho.

### Lead excluído e atribuição manual

Lead excluído logicamente fica fora da fila, dos lotes, da carga e da redistribuição. O histórico já gravado permanece. A exclusão não gera `assignment` nem `LeadAtribuido`.

Atribuição manual aceita qualquer vendedor `ACTIVE`. Limite de leads abertos e `participa_distribuicao` barram as regras e a fila, não a atribuição manual.

### Lista sem dono

A lista mostra só lead `ABERTA`, sem dono e sem exclusão. Os filtros informados valem juntos: UF, cidade, quente e origem.

### Parado e e-mail

Com a devolução de parado ligada, o lead volta à fila e a feature envia e-mail ao vendedor que era o dono, no e-mail cadastrado dele. A troca de dono continua gravada mesmo que o envio ainda esteja em andamento.

A falha do e-mail não desfaz a volta à fila. O lead permanece sem dono, com `assignment` e `LeadAtribuido`. A falha fica apenas no registro do envio.

### Fila

1. Leads quentes, do criado há mais tempo ao mais novo. Devolução e parado não tiram o lead desse grupo.
2. Leads não quentes que não foram devolvidos nem voltaram por parado, também do criado há mais tempo ao mais novo.
3. No fim, leads não quentes devolvidos ou que voltaram por parado, do criado há mais tempo ao mais novo.

Dois vendedores não pegam o mesmo lead.

### Dono e histórico

Um lead tem no máximo um dono por vez. Dono nulo significa fila.

Toda troca de dono gera uma linha em `assignment` e publica `LeadAtribuido`. Quem permanece na fila, sem troca, não gera linha.

### Requisitos de origem que permanecem

Estes itens vieram do pedido inicial e continuam válidos onde uma decisão acima não os afunilou.

| ID | Requisito que permanece |
| --- | --- |
| RF-DS-01 | Admin vê leads sem dono, com os filtros decididos. |
| RF-DS-02 | Admin atribui leads selecionados a um vendedor. |
| RF-DS-03 | Admin distribui em lote por rodízio, rodízio com peso, carga ou território. |
| RF-DS-04 | Admin vê a simulação de quantos leads cada vendedor recebe antes de confirmar. |
| RF-DS-05 | Admin configura peso, limite de leads abertos e participação de cada vendedor. |
| RF-DS-06 | Admin configura territórios (UF ou cidade para vendedor). |
| RF-DS-07 | Admin ativa distribuição automática para leads novos, com a regra escolhida. |
| RF-DS-08 | Admin redistribui a carteira de um vendedor pelo lote decidido acima. |
| RF-DS-09 | Vendedor pega o próximo lead da fila. |
| RF-DS-10 | Vendedor devolve um lead para a fila com motivo. |
| RF-DS-11 | Admin vê o histórico de donos de um lead. |
| RN-DS-01 | Um lead tem no máximo um dono por vez. |
| RN-DS-02 | Só vendedor `ACTIVE` e com `participa_distribuicao = true` recebe leads por regra. Atribuição manual aceita qualquer vendedor ativo. |
| RN-DS-03 | Rodízio segue a ordem de cadastro e continua de onde parou (ponteiro salvo). |
| RN-DS-04 | No rodízio com peso, peso 2 recebe o dobro de peso 1; peso 0 não recebe. |
| RN-DS-05 | Por carga, vai para quem tem menos leads `ABERTA`; empate decide pelo rodízio. |
| RN-DS-06 | Por território, cidade tem prioridade sobre UF; sem correspondente, cai no rodízio. |
| RN-DS-07 | Quem atingiu `max_leads_abertos` é pulado nas regras e não pega da fila. |
| RN-DS-08 | Dois vendedores não pegam o mesmo lead. A ordem da fila é a decidida acima. |
| RN-DS-09 | Lead `GANHO` ou `PERDIDO` não é redistribuído. |
| RN-DS-10 | Sem atividade há mais de N dias (padrão 5) marca parado. «Atividade» aqui é o tempo desde `atribuido_em`. Com a opção ligada, volta à fila e o vendedor anterior recebe e-mail. |
| RN-DS-11 | Ao desativar um vendedor, os leads abertos aparecem para o admin redistribuir; nada se move sozinho. |
| RN-DS-12 | Toda troca de dono gera `assignment` e publica `LeadAtribuido`. |

Tabelas de origem, já com o recorte por empresa:

- `lead_ownership`: `lead_id` (PK), `owner_id` nulo na fila, `atribuido_em`, `parado_desde` nulo.
- `assignment`: `id`, `lead_id`, `de_user_id`, `para_user_id`, `regra` (`MANUAL`, `RODIZIO`, `PESO`, `CARGA`, `TERRITORIO`, `FILA`, `DEVOLUCAO`, `PARADO`), `motivo` obrigatório em `DEVOLUCAO`, `por_user_id` nulo se automático, `criado_em`.
- `seller_distribution_config`: `user_id` (PK), `peso` padrão 1 (0 a 10), `max_leads_abertos` nulo sem limite, `participa_distribuicao` padrão true.
- `territory`: `id`, `uf`, `cidade` nula para a UF inteira, `user_id`. Par (`uf`, `cidade`) único por empresa.
- `distribution_settings`, uma linha por empresa: `auto_distribuir` padrão false, `regra_padrao` (`RODIZIO`, `PESO`, `CARGA`, `TERRITORIO`, `FILA`), `dias_para_parado` padrão 5, `devolver_parado_a_fila` padrão false, `ponteiro_rodizio`.

## Dúvidas pendentes

Nenhuma de produto.

## Alternativas avaliadas

### O que entra na feature

- **Só o dono.** Menor e alinhada ao propósito, mas carga, território, fila quente e parado não teriam o que ler.
- **B amplo.** Funil, score e atividades dentro da Distribuição. Acoplamento alto e escopo de outro módulo.
- **B mínimo, escolha.** As regras executam. Parado não vê trabalho real do vendedor, e a situação não é um funil.

### Quem grava situação, quente, UF e cidade

- **Só o admin.** O vendedor não atualiza o resultado do próprio lead, e a carga deixa de refletir o que ele já concluiu.
- **Admin nos quatro; vendedor dono em situação e quente. Escolha.** UF e cidade ficam com quem configura território.
- **Ninguém nesta feature.** Quente, ganho e perdido nunca mudariam.

### Reabrir ganho ou perdido

- **Definitivo. Escolha.** Uma marcação errada permanece.
- **Só o admin reabre.** Permite corrigir, e a carga volta a contar o lead.
- **Admin e vendedor dono reabrem.** O vendedor devolve um lead concluído para a carteira aberta.

### Território com vendedor impedido

- **Cai no rodízio. Escolha.** O território não segura o lead.
- **Espera na fila por aquele vendedor.** A cobertura geográfica atrasa a distribuição.
- **Tenta a UF inteira antes do rodízio.** A prioridade geográfica dura mais uma etapa.

### Lote com lead sem destino

- **Atribui quem tem destino e deixa o restante na fila. Escolha.** A simulação separa as duas quantidades.
- **Um lead sem destino impede o lote inteiro.**
- **Leads sem destino voltam como erro e não permanecem na fila.**

### Lead excluído

- **Fica fora, sem novo assignment. Escolha.** Exclusão e dono ficam separados.
- **A exclusão também devolve à fila e publica `LeadAtribuido`.**
- **A exclusão é recusada enquanto houver dono ou fila.**

### Aviso do parado

- **Sem mensagem.** O vendedor só veria a perda ao abrir o lead.
- **E-mail ao dono anterior. Escolha.** Depende do endereço cadastrado.
- **Outro canal.** Esta feature não o definiria.

### Filtros da lista sem dono

- **UF, cidade, quente e origem. Escolha.**
- **Os mesmos, mais período de criação.**
- **Só paginação.**

### Falha do e-mail

- **Não desfaz a volta à fila. Escolha.** O vendedor pode não ser avisado.
- **Sem e-mail enviado, o lead continua com o mesmo dono.**
- **A troca só fica definitiva depois que o e-mail é aceito.**

### Redistribuição da carteira

- **Mesmo lote, com simulação, só sobre leads `ABERTA`. Escolha.** Desativar o vendedor não move nada sozinho.
- **Só atribuição manual, sem as regras.**
- **Lote e atribuição manual como duas ações de redistribuição.**

### Ordem da fila, entre os não quentes

- **Criado há mais tempo, mesmo que tenha voltado.** Um lead antigo devolvido passa na frente de um lead novo.
- **A espera recomeça na última entrada na fila.**
- **Criado há mais tempo, com devolvido e parado no fim. Escolha.**

## Critérios preliminares de aceitação

- Um lead tem no máximo um dono.
- Toda troca de dono fica no histórico e publica `LeadAtribuido`.
- Lote e redistribuição só trocam dono depois da simulação confirmada.
- A simulação mostra quantos leads cada vendedor recebe e quantos continuam sem dono.
- `GANHO`, `PERDIDO` e lead excluído não mudam de dono por regra.
- Depois de `GANHO` ou `PERDIDO`, nenhuma ação desta feature devolve o lead para `ABERTA`.
- Vendedor no limite, inativo ou fora da distribuição não recebe por regra nem pega da fila.
- Vendedor de território impedido não recebe por essa regra, e o lead segue para o rodízio.
- Dois vendedores não pegam o mesmo lead.
- A fila segue a ordem decidida.
- O admin restringe a lista sem dono por UF, cidade, quente e origem, e não vê nela lead ganho, perdido ou excluído.
- Vendedor que não é o dono não altera situação nem quente. UF e cidade só mudam por ação do admin.
- Quando a opção está ligada e o lead parado perde o dono, o vendedor anterior é destinatário de um e-mail.
- Falha de e-mail não devolve o lead ao dono anterior nem apaga o histórico da troca.
- Redistribuir a carteira mostra a simulação dos leads `ABERTA` daquele vendedor e só troca o dono depois da confirmação.

## Alterações de escopo

Nenhuma.
