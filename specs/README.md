# Especificações

Índice do fluxo por bounded context. Não é a norma completa das features.

A norma já escrita está em `docs/features/<slug>/` (`discovery.md`, `spec.md`, `validation.md`, `plan.md`). Essa pasta está no `.gitignore`. Busca automática pode não listá-la; abra o caminho.

Este diretório guarda rastreabilidade, classificação e lacunas. Ele não repete o texto das regras. Transcrever sem uma passagem do `spec-generator` criaria uma segunda norma.

## Fluxo

1. Ler a fonte em `docs/features/` ou o requisito novo apontado pelo usuário.
2. `domain-analyst` classifica e lista perguntas. Não grava arquivo.
3. `spec-generator` atualiza os arquivos deste diretório, só com o que a fonte diz.
4. `spec-validator` continua responsável por `docs/features/<slug>/validation.md` quando a norma for uma spec de feature.
5. Aprovação humana para decisão de negócio e para item `OPEN` que mude comportamento.
6. `architect` só então decompõe tarefa `READY`.

Classificação: `CONFIRMED`, `INFERRED`, `OPEN`, `CONFLICTING`.

Arquivo presente não significa especificação completa.

## Contextos

| Contexto | Pacote | Fonte de feature |
| --- | --- | --- |
| identidade | `tech.techparterns.identidade` | `docs/features/identidade-e-acesso/` |
| lead | `tech.techparterns.lead` | `docs/features/leads-e-importacao/` |
| distribuicao | `tech.techparterns.distribuicao` | `docs/features/distribuicao/` |

`shared` é núcleo técnico, descrito em `architecture/module-map.md`.
