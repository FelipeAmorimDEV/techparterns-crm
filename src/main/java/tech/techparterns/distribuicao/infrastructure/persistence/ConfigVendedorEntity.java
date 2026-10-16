package tech.techparterns.distribuicao.infrastructure.persistence;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "seller_distribution_config")
public class ConfigVendedorEntity {

    @EmbeddedId
    public Chave id;

    @Column(name = "peso", nullable = false)
    public int peso;

    @Column(name = "max_leads_abertos")
    public Integer maxLeadsAbertos;

    @Column(name = "participa_distribuicao", nullable = false)
    public boolean participa;

    @Embeddable
    public static class Chave implements Serializable {
        @Column(name = "company_id")
        public String companyId;
        @Column(name = "user_id")
        public UUID userId;

        @Override
        public boolean equals(Object o) {
            return o instanceof Chave c && Objects.equals(companyId, c.companyId) && Objects.equals(userId, c.userId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(companyId, userId);
        }
    }
}
