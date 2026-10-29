# Identidade — contratos

Estado: contratos HTTP não inventariados nesta sessão.

Código de borda encontrado:

- `src/main/java/tech/techparterns/identidade/infrastructure/rest/AuthResource.java`
- `UsuarioResource.java`
- `PerfilResource.java`
- `ConviteResource.java`
- `PlataformaEmpresaResource.java`

Dependências de pacote observadas estão em `specs/architecture/dependency-rules.md`. A porta `VendedoresDistribuicao` é implementada na infraestrutura de identidade e consumida por distribuição.

Não foi produzida tabela de endpoint, papel e payload. Isso fica `OPEN` até o `spec-generator` extrair da spec, sem completar o que a spec não definir.
