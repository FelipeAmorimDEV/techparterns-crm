package tech.techparterns.identidade.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "platform_user")
public class DonoPlataformaEntity extends BaseEntity {

    @Column(name = "nome", nullable = false) public String nome;
    @Column(name = "email", nullable = false) public String email;
    @Column(name = "password_hash") public String passwordHash;
    @Column(name = "ultimo_login_em") public Instant ultimoLoginEm;
}
