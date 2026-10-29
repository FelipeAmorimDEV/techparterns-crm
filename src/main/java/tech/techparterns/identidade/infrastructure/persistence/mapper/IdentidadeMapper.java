package tech.techparterns.identidade.infrastructure.persistence.mapper;

import java.time.Instant;

import tech.techparterns.identidade.domain.model.*;
import tech.techparterns.identidade.infrastructure.persistence.*;

/** Conversões domínio ↔ entidade JPA. {@code toDomain} sempre usa {@code reconstituir}, nunca {@code tryCreate}. */
public final class IdentidadeMapper {

    private IdentidadeMapper() {}

    private static <E extends BaseEntity> E base(E e, tech.techparterns.shared.domain.Entity d) {
        e.id = d.getId();
        e.createdAt = d.getCreatedAt();
        e.updatedAt = Instant.now();
        e.deletedAt = d.getDeletedAt();
        return e;
    }

    // Empresa
    public static Empresa toDomain(EmpresaEntity e) {
        return Empresa.reconstituir(e.id, e.createdAt, e.updatedAt, e.deletedAt, NomeEmpresa.reconstituir(e.nome),
                StatusEmpresa.valueOf(e.status), e.suspensaEm,
                e.motivoSuspensao == null ? null : MotivoSuspensao.reconstituir(e.motivoSuspensao));
    }

    public static EmpresaEntity toEntity(Empresa d) {
        var e = base(new EmpresaEntity(), d);
        e.nome = d.nome().valor();
        e.status = d.status().name();
        e.suspensaEm = d.suspensaEm();
        e.motivoSuspensao = d.motivoSuspensao() == null ? null : d.motivoSuspensao().valor();
        return e;
    }

    // Usuário de empresa
    public static UsuarioEmpresa toDomain(UsuarioEmpresaEntity e) {
        return UsuarioEmpresa.reconstituir(e.id, e.createdAt, e.updatedAt, e.deletedAt, e.empresaId,
                NomeUsuario.reconstituir(e.nome), EmailUsuario.reconstituir(e.email),
                e.telefone == null ? null : TelefoneUsuario.reconstituir(e.telefone),
                e.passwordHash == null ? null : HashSenha.reconstituir(e.passwordHash),
                PapelUsuario.valueOf(e.papel), StatusUsuario.valueOf(e.status), e.ultimoLoginEm);
    }

    public static UsuarioEmpresaEntity toEntity(UsuarioEmpresa d) {
        var e = base(new UsuarioEmpresaEntity(), d);
        e.empresaId = d.empresaId();
        e.nome = d.nome().valor();
        e.email = d.email().valor();
        e.telefone = d.telefone() == null ? null : d.telefone().valor();
        e.passwordHash = d.hashSenha() == null ? null : d.hashSenha().valor();
        e.papel = d.papel().name();
        e.status = d.status().name();
        e.ultimoLoginEm = d.ultimoLoginEm();
        return e;
    }

    // Dono
    public static DonoPlataforma toDomain(DonoPlataformaEntity e) {
        return DonoPlataforma.reconstituir(e.id, e.createdAt, e.updatedAt, e.deletedAt,
                NomeUsuario.reconstituir(e.nome), EmailUsuario.reconstituir(e.email),
                e.passwordHash == null ? null : HashSenha.reconstituir(e.passwordHash), e.ultimoLoginEm);
    }

    public static DonoPlataformaEntity toEntity(DonoPlataforma d) {
        var e = base(new DonoPlataformaEntity(), d);
        e.nome = d.nome().valor();
        e.email = d.email().valor();
        e.passwordHash = d.hashSenha() == null ? null : d.hashSenha().valor();
        e.ultimoLoginEm = d.ultimoLoginEm();
        return e;
    }

    // Convite
    public static Convite toDomain(ConviteEntity e) {
        return Convite.reconstituir(e.id, e.createdAt, e.updatedAt, e.deletedAt, e.empresaId,
                e.email == null ? null : EmailUsuario.reconstituir(e.email), HashToken.reconstituir(e.tokenHash.trim()),
                e.expiraEm, e.maxUsos, e.usos, e.revogadoEm, e.criadoPor);
    }

    public static ConviteEntity toEntity(Convite d) {
        var e = base(new ConviteEntity(), d);
        e.empresaId = d.empresaId();
        e.email = d.email() == null ? null : d.email().valor();
        e.tokenHash = d.hashToken().valor();
        e.expiraEm = d.expiraEm();
        e.maxUsos = d.maxUsos();
        e.usos = d.usos();
        e.revogadoEm = d.revogadoEm();
        e.criadoPor = d.criadoPor();
        return e;
    }

    // Token de senha
    public static TokenSenha toDomain(TokenSenhaEntity e) {
        return TokenSenha.reconstituir(e.id, e.createdAt, e.updatedAt, e.deletedAt, e.usuarioId,
                TipoTokenSenha.valueOf(e.tipo), HashToken.reconstituir(e.tokenHash.trim()), e.expiraEm,
                e.usadoEm, e.invalidadoEm);
    }

    public static TokenSenhaEntity toEntity(TokenSenha d) {
        var e = base(new TokenSenhaEntity(), d);
        e.usuarioId = d.usuarioId();
        e.tipo = d.tipo().name();
        e.tokenHash = d.hashToken().valor();
        e.expiraEm = d.expiraEm();
        e.usadoEm = d.usadoEm();
        e.invalidadoEm = d.invalidadoEm();
        return e;
    }

    // Refresh token
    public static RefreshToken toDomain(RefreshTokenEntity e) {
        return RefreshToken.reconstituir(e.id, e.createdAt, e.updatedAt, e.deletedAt,
                TitularTipo.valueOf(e.titularTipo), e.titularId, e.familiaId,
                HashToken.reconstituir(e.tokenHash.trim()), e.expiraEm, e.rotacionadoEm, e.revogadoEm);
    }

    public static RefreshTokenEntity toEntity(RefreshToken d) {
        var e = base(new RefreshTokenEntity(), d);
        e.titularTipo = d.titularTipo().name();
        e.titularId = d.titularId();
        e.familiaId = d.familiaId();
        e.tokenHash = d.hashToken().valor();
        e.expiraEm = d.expiraEm();
        e.rotacionadoEm = d.rotacionadoEm();
        e.revogadoEm = d.revogadoEm();
        return e;
    }

    // Evento de acesso
    public static EventoAcesso toDomain(EventoAcessoEntity e) {
        return EventoAcesso.reconstituir(e.id, e.createdAt, e.updatedAt, e.deletedAt, e.tipo, e.resultado,
                e.titularTipo == null ? null : TitularTipo.valueOf(e.titularTipo), e.titularId, e.empresaId, e.ip,
                e.ocorridoEm, e.detalhe);
    }

    public static EventoAcessoEntity toEntity(EventoAcesso d) {
        var e = base(new EventoAcessoEntity(), d);
        e.tipo = d.tipo();
        e.resultado = d.resultado();
        e.titularTipo = d.titularTipo() == null ? null : d.titularTipo().name();
        e.titularId = d.titularId();
        e.empresaId = d.empresaId();
        e.ip = d.ip();
        e.ocorridoEm = d.ocorridoEm();
        e.detalhe = d.detalhe();
        return e;
    }

    // Envio de e-mail
    public static EnvioEmail toDomain(EnvioEmailEntity e) {
        return EnvioEmail.reconstituir(e.id, e.createdAt, e.updatedAt, e.deletedAt, e.usuarioId,
                TipoTokenSenha.valueOf(e.tipo), StatusEnvioEmail.valueOf(e.status), e.tentativas);
    }

    public static EnvioEmailEntity toEntity(EnvioEmail d) {
        var e = base(new EnvioEmailEntity(), d);
        e.usuarioId = d.usuarioId();
        e.tipo = d.tipo().name();
        e.status = d.status().name();
        e.tentativas = d.tentativas();
        return e;
    }
}
