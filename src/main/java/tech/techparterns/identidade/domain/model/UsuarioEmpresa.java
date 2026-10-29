package tech.techparterns.identidade.domain.model;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

import tech.techparterns.shared.domain.Entity;
import tech.techparterns.shared.domain.Result;

public class UsuarioEmpresa extends Entity {

    private final UUID empresaId;
    private NomeUsuario nome;
    private EmailUsuario email;
    private TelefoneUsuario telefone;
    private HashSenha hashSenha;
    private PapelUsuario papel;
    private StatusUsuario status;
    private Instant ultimoLoginEm;

    private UsuarioEmpresa(UUID id, Instant createdAt, Instant updatedAt, OffsetDateTime deletedAt,
                           UUID empresaId, NomeUsuario nome, EmailUsuario email, TelefoneUsuario telefone,
                           HashSenha hashSenha, PapelUsuario papel, StatusUsuario status, Instant ultimoLoginEm) {
        super(id, createdAt, updatedAt, deletedAt);
        this.empresaId = empresaId;
        this.nome = nome;
        this.email = email;
        this.telefone = telefone;
        this.hashSenha = hashSenha;
        this.papel = papel;
        this.status = status;
        this.ultimoLoginEm = ultimoLoginEm;
    }

    /** Nasce PENDING e sem senha; o telefone é opcional. */
    public static Result<UsuarioEmpresa> tryCreate(UUID empresaId, NomeUsuario nome, EmailUsuario email,
                                                   TelefoneUsuario telefone, PapelUsuario papel) {
        var erro = validar(empresaId, nome, email, papel);
        if (erro != null)
            return Result.fail(erro);
        var agora = Instant.now();
        return Result.ok(new UsuarioEmpresa(UUID.randomUUID(), agora, agora, null, empresaId, nome, email,
                telefone, null, papel, StatusUsuario.PENDING, null));
    }

    /** Aceite de convite: SELLER já ACTIVE, com senha. */
    public static Result<UsuarioEmpresa> criarAtivoPorConvite(UUID empresaId, NomeUsuario nome, EmailUsuario email,
                                                              TelefoneUsuario telefone, HashSenha hash) {
        var erro = validar(empresaId, nome, email, PapelUsuario.SELLER);
        if (erro != null || hash == null)
            return Result.fail(erro != null ? erro : ErroIdentidade.SENHA_OBRIGATORIA.mensagem());
        var agora = Instant.now();
        return Result.ok(new UsuarioEmpresa(UUID.randomUUID(), agora, agora, null, empresaId, nome, email,
                telefone, hash, PapelUsuario.SELLER, StatusUsuario.ACTIVE, null));
    }

    private static String validar(UUID empresaId, NomeUsuario nome, EmailUsuario email, PapelUsuario papel) {
        if (empresaId == null)
            return tech.techparterns.shared.domain.exception.ErroDominio.EMPRESA_OBRIGATORIA.mensagem();
        if (nome == null)
            return ErroIdentidade.NOME_OBRIGATORIO.mensagem();
        if (email == null)
            return ErroIdentidade.EMAIL_OBRIGATORIO.mensagem();
        if (papel == null)
            return ErroIdentidade.PAPEL_INVALIDO.mensagem();
        return null;
    }

    public static UsuarioEmpresa reconstituir(UUID id, Instant createdAt, Instant updatedAt, OffsetDateTime deletedAt,
                                              UUID empresaId, NomeUsuario nome, EmailUsuario email,
                                              TelefoneUsuario telefone, HashSenha hashSenha, PapelUsuario papel,
                                              StatusUsuario status, Instant ultimoLoginEm) {
        return new UsuarioEmpresa(id, createdAt, updatedAt, deletedAt, empresaId, nome, email, telefone,
                hashSenha, papel, status, ultimoLoginEm);
    }

    /** PENDING vira ACTIVE; ACTIVE permanece. INACTIVE não pode definir senha. */
    public Result<Void> definirSenha(HashSenha novoHash) {
        if (status == StatusUsuario.INACTIVE)
            return Result.fail(ErroIdentidade.TOKEN_SENHA_INVALIDO.mensagem());
        this.hashSenha = novoHash;
        this.status = StatusUsuario.ACTIVE;
        return Result.ok();
    }

    public void desativar() { this.status = StatusUsuario.INACTIVE; }

    /** Com senha volta a ACTIVE; sem senha vai a PENDING (DEL-05). */
    public Result<StatusUsuario> reativar() {
        if (status != StatusUsuario.INACTIVE)
            return Result.fail(ErroIdentidade.USUARIO_NAO_INATIVO.mensagem());
        this.status = hashSenha != null ? StatusUsuario.ACTIVE : StatusUsuario.PENDING;
        return Result.ok(this.status);
    }

    public void alterarPapel(PapelUsuario novo) { this.papel = novo; }

    public void editarContato(NomeUsuario novoNome, TelefoneUsuario novoTelefone) {
        if (novoNome != null)
            this.nome = novoNome;
        if (novoTelefone != null)
            this.telefone = novoTelefone;
    }

    public void alterarEmail(EmailUsuario novo) { this.email = novo; }

    public void registrarLogin(Instant agora) { this.ultimoLoginEm = agora; }

    public boolean podeAutenticar() { return status == StatusUsuario.ACTIVE && hashSenha != null; }

    public boolean ehAdminAtivo() { return papel == PapelUsuario.ADMIN && status == StatusUsuario.ACTIVE; }

    public UUID empresaId() { return empresaId; }
    public NomeUsuario nome() { return nome; }
    public EmailUsuario email() { return email; }
    public TelefoneUsuario telefone() { return telefone; }
    public HashSenha hashSenha() { return hashSenha; }
    public PapelUsuario papel() { return papel; }
    public StatusUsuario status() { return status; }
    public Instant ultimoLoginEm() { return ultimoLoginEm; }
}
