# Lead — contratos

Estado: contrato HTTP não inventariado.

Borda encontrada: `src/main/java/tech/techparterns/lead/infrastructure/rest/LeadResource.java`.

Acoplamento observado com distribuição: `LeadResource` importa tipos de `distribuicao.application`, e `FatosLeadImpl` implementa `distribuicao.domain.port.FatosLead`. Detalhe em `specs/architecture/dependency-rules.md`.

A spec de lead e a spec de distribuição falam dos fatos do lead (situação, UF, cidade, quente). A fronteira exata entre os dois documentos não foi reconciliada nesta sessão. Trate sobreposição não conferida como `OPEN` até o `domain-analyst` comparar os dois textos.
