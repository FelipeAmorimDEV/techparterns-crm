# Regras de dependência

Observação de imports em 2026-10-24. Não é uma decisão nova de refatoração.

## O que o código faz

- `shared` não depende dos contextos de negócio.
- Contextos de negócio dependem de `shared`.
- `lead` importa `distribuicao.application` em `LeadResource` e `distribuicao.domain.port.FatosLead` em `FatosLeadImpl`.
- `distribuicao` importa `lead.domain.model.Lead`, `lead.domain.model.SituacaoLead` e `lead.domain.port.DistribuirLeadNovo`.
- `distribuicao.infrastructure.mail.AvisoParadoListener` importa `identidade.domain.port.EnviadorEmail`.
- `distribuicao.infrastructure.mail.RegistroEnvioParadoImpl` importa `identidade.infrastructure.persistence.EnvioEmailEntity`.
- `identidade.infrastructure.persistence.VendedoresDistribuicaoImpl` importa `distribuicao.domain.port.VendedoresDistribuicao`.

## Como classificar

| Enunciado | Classificação |
| --- | --- |
| Esses imports existiam nos arquivos citados na data da inspeção | `CONFIRMED` |
| O import de `EnvioEmailEntity` atravessa a infraestrutura de outro contexto | `CONFIRMED` como leitura do import |
| Esse acoplamento deve ser desfeito agora | `OPEN`. Nenhuma spec lida nesta sessão mandou refatorar isso nesta configuração |
| Pacotes equivalem a módulos Maven independentes | `CONFLICTING` com o POM, que tem um módulo só |

Não há teste automatizado de arquitetura para impedir dependência nova.

Tarefas não podem alterar ao mesmo tempo um contrato usado pelos dois lados (`FatosLead`, `DistribuirLeadNovo`, `VendedoresDistribuicao`, `EnviadorEmail`) sem ordem explícita no plano.
