package tech.techparterns.distribuicao.domain.model;

public enum RegraAtribuicao {
    MANUAL, RODIZIO, PESO, CARGA, TERRITORIO, FILA, DEVOLUCAO, PARADO;

    public boolean regraDeLote() {
        return this == RODIZIO || this == PESO || this == CARGA || this == TERRITORIO;
    }

    public boolean regraAutomatica() {
        return regraDeLote() || this == FILA;
    }
}
