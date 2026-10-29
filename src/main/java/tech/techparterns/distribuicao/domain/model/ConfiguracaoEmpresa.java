package tech.techparterns.distribuicao.domain.model;

public record ConfiguracaoEmpresa(
        String companyId,
        boolean automatica,
        RegraAtribuicao regraPadrao,
        int diasParaParado,
        boolean devolverParadoAFila,
        Integer ponteiro) {

    public static ConfiguracaoEmpresa padrao(String companyId) {
        return new ConfiguracaoEmpresa(companyId, false, null, 5, false, null);
    }
}
