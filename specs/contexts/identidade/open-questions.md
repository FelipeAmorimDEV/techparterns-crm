# Identidade — perguntas abertas

Extraídas do relatório `docs/features/identidade-e-acesso/validation.md` (16/10/2026), seção 8. A pergunta não foi respondida aqui.

| ID | Pergunta registrada pelo validador | Efeito | Classificação |
| --- | --- | --- | --- |
| DEL-18 | `size` fora de 1 a 100 responde 400 | Muda contrato de listagem | `OPEN` |
| DEL-19 | Dono vê o status do envio de e-mail do admin PENDING | Muda o que a listagem mostra | `OPEN` |
| DEL-20 | Só requisição que passa na validação conta no limite de 30 por hora | Muda o rate limit | `OPEN` |
| DEL-21 | `role` omitido equivale a `SELLER`; valor inválido responde 400 | Muda o cadastro | `OPEN` |
| MN-O3 | Qual admin PENDING a listagem do dono mostra quando há vários | Muda a listagem | `OPEN` |
| MN-O4 | O contador do par e-mail e IP tem janela de tempo | Muda o bloqueio de login | `OPEN` |
| Q-01, Q-03 | Retenção e expurgo | O validador diz que não bloqueiam requisito atual | `OPEN`, não bloqueante segundo o relatório |

MN-O1, MN-O2, MN-O5 e MN-O6 são melhorias de texto ou de critério citadas no mesmo relatório. Não foram resolvidas aqui.

R-01 é risco de teste registrado na validação, não uma regra de negócio nova.
