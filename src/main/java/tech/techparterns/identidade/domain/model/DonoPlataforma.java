package tech.techparterns.identidade.domain.model;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

import tech.techparterns.shared.domain.Entity;
import tech.techparterns.shared.domain.Result;

/** Dono do SaaS (PLATFORM_OWNER). Nasce sem senha; a senha só é definida pelo comando manual. */
public class DonoPlataforma extends Entity {

    private final NomeUsuario nome;
    private final EmailUsuario email;
    private HashSenha hashSenha;
    private Instant ultimoLoginEm;

    private DonoPlataforma(UUID id, Instant createdAt, Instant updatedAt, OffsetDateTime deletedAt,
                           NomeUsuario nome, EmailUsuario email, HashSenha hashSenha, Instant ultimoLoginEm) {
        super(id, createdAt, updatedAt, deletedAt);
        this.nome = nome;
        this.email = email;
        this.hashSenha = hashSenha;
        this.ultimoLoginEm = ultimoLoginEm;
    }

    public static Result<DonoPlataforma> tryCreate(NomeUsuario nome, EmailUsuario email) {
        if (nome == null)
            return Result.fail(ErroIdentidade.NOME_OBRIGATORIO.mensagem());
        if (email == null)
            return Result.fail(ErroIdentidade.EMAIL_OBRIGATORIO.mensagem());
        var agora = Instant.now();
        return Result.ok(new DonoPlataforma(UUID.randomUUID(), agora, agora, null, nome, email, null, null));
    }

    public static DonoPlataforma reconstituir(UUID id, Instant createdAt, Instant updatedAt, OffsetDateTime deletedAt,
                                              NomeUsuario nome, EmailUsuario email, HashSenha hashSenha,
                                              Instant ultimoLoginEm) {
        return new DonoPlataforma(id, createdAt, updatedAt, deletedAt, nome, email, hashSenha, ultimoLoginEm);
    }

    public void definirSenha(HashSenha novo) { this.hashSenha = novo; }

    public void registrarLogin(Instant agora) { this.ultimoLoginEm = agora; }

    public boolean podeAutenticar() { return hashSenha != null; }

    public NomeUsuario nome() { return nome; }
    public EmailUsuario email() { return email; }
    public HashSenha hashSenha() { return hashSenha; }
    public Instant ultimoLoginEm() { return ultimoLoginEm; }
}
