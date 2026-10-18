package tech.techparterns.distribuicao.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "distribution_settings")
public class ConfiguracaoEmpresaEntity {

    @Id
    @Column(name = "company_id")
    public String companyId;

    @Column(name = "auto_distribuicao", nullable = false)
    public boolean autoDistribuicao;

    @Column(name = "regra_padrao")
    public String regraPadrao;

    @Column(name = "dias_para_parado", nullable = false)
    public int diasParaParado;

    @Column(name = "devolver_parado_a_fila", nullable = false)
    public boolean devolverParadoAFila;

    @Column(name = "ponteiro")
    public Integer ponteiro;
}
