# Mapa de módulos

Há um único módulo Maven. O mapa abaixo é de pacotes, lido em 2026-10-24.

| Pacote | Papel observado no código | Resources JAX-RS encontrados |
| --- | --- | --- |
| `tech.techparterns.shared` | Núcleo: `Entity`, `Result`, `CompanyId`, `UseCase`, segurança de tenant, erros | `shared/infrastructure/security/DevTokenResource.java` |
| `tech.techparterns.identidade` | Empresas, usuários, sessão, convite, e-mail de acesso | `AuthResource`, `UsuarioResource`, `PerfilResource`, `ConviteResource`, `PlataformaEmpresaResource` |
| `tech.techparterns.lead` | Lead e importação CSV | `LeadResource` |
| `tech.techparterns.distribuicao` | Dono, regras, território, parado | `DistribuicaoResource` |
| `tech.techparterns` | Resource de exemplo | `GreetingResource` |

`shared` não importa `identidade`, `lead` nem `distribuicao`.

Skills que descrevem essas camadas estão em `.cursor/skills/`: `entity`, `value-object`, `domain-service`, `repository`, `use-case`, `dto`, `query-cqrs`, `resource`. Não há skill com o nome aggregate, controller, persistence ou test. Controller corresponde à skill `resource`. Persistência corresponde à skill `repository` mais as migrações Flyway.
