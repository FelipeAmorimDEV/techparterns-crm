# Distribuição — contratos

Estado: contrato HTTP não inventariado.

Borda encontrada: `src/main/java/tech/techparterns/distribuicao/infrastructure/rest/DistribuicaoResource.java`.

Portas observadas entre contextos:

- `lead.domain.port.DistribuirLeadNovo`, implementada em `distribuicao.application.DistribuirLeadNovoImpl`
- `distribuicao.domain.port.FatosLead`, implementada em `lead.infrastructure.persistence.FatosLeadImpl`
- `distribuicao.domain.port.VendedoresDistribuicao`, implementada em `identidade.infrastructure.persistence.VendedoresDistribuicaoImpl`
- `identidade.domain.port.EnviadorEmail`, usada por `AvisoParadoListener`
- `RegistroEnvioParadoImpl` importa `identidade.infrastructure.persistence.EnvioEmailEntity`

Esses imports são fato de código. Não são uma decisão de mantê-los.
