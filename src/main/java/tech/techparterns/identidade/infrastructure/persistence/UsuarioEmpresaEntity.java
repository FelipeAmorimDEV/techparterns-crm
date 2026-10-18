package tech.techparterns.identidade.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "app_user")
public class UsuarioEmpresaEntity extends BaseEntity {

    @Column(name = "company_id", nullable = false) public UUID empresaId;
    @Column(name = "nome", nullable = false) public String nome;
    @Column(name = "email", nullable = false) public String email;
    @Column(name = "telefone") public String telefone;
    @Column(name = "password_hash") public String passwordHash;
    @Column(name = "role", nullable = false) public String papel;
    @Column(name = "status", nullable = false) public String status;
    @Column(name = "ultimo_login_em") public Instant ultimoLoginEm;
}
