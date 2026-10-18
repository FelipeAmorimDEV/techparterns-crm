package tech.techparterns.identidade.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import tech.techparterns.identidade.domain.model.EmailUsuario;
import tech.techparterns.identidade.domain.model.PapelUsuario;
import tech.techparterns.identidade.domain.model.StatusUsuario;
import tech.techparterns.identidade.domain.model.UsuarioEmpresa;

public interface UsuarioEmpresaRepository {

    void salvar(UsuarioEmpresa usuario);

    /** Filtra por id e empresa: usuário de outra empresa não é encontrado (DEL-11). */
    Optional<UsuarioEmpresa> buscarPorId(UUID id, UUID empresaId);

    /** Sem filtro de empresa: usar só em fluxos por token (SETUP, RESET, refresh). */
    Optional<UsuarioEmpresa> buscarPorIdSemEmpresa(UUID id);

    /** Sem filtro de empresa: login e recuperação acontecem antes de existir empresa no token. */
    Optional<UsuarioEmpresa> buscarPorEmail(EmailUsuario email);

    boolean existeEmail(EmailUsuario email);

    /** Bloqueia (FOR UPDATE, ordenado por id) todas as linhas ADMIN da empresa e as devolve já atualizadas. */
    List<UsuarioEmpresa> bloquearAdminsDaEmpresa(UUID empresaId);

    List<UsuarioEmpresa> listar(UUID empresaId, StatusUsuario status, PapelUsuario papel, int page, int size);

    long contar(UUID empresaId, StatusUsuario status, PapelUsuario papel);
}
