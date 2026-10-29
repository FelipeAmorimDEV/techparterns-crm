package tech.techparterns.distribuicao.infrastructure.persistence;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "territory")
public class TerritorioEntity {

    @Id
    @Column(name = "id")
    public UUID id;

    @Column(name = "company_id", nullable = false)
    public String companyId;

    @Column(name = "uf", nullable = false)
    public String uf;

    @Column(name = "cidade")
    public String cidade;

    @Column(name = "user_id", nullable = false)
    public UUID userId;
}
