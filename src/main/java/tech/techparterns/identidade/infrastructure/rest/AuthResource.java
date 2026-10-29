package tech.techparterns.identidade.infrastructure.rest;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import jakarta.annotation.security.PermitAll;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import tech.techparterns.identidade.application.command.*;
import tech.techparterns.identidade.application.dto.SessaoDto;
import tech.techparterns.identidade.application.service.SessaoService.SessaoEmitida;
import tech.techparterns.identidade.application.usecase.*;
import tech.techparterns.identidade.infrastructure.security.CookieRefresh;
import tech.techparterns.identidade.infrastructure.security.OrigemPermitida;

@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Tag(name = "Autenticação", description = "Login unificado, sessão e senha.")
public class AuthResource {

    private final LoginUseCase login;
    private final RenovarSessaoUseCase renovar;
    private final EncerrarSessaoUseCase encerrar;
    private final SolicitarRecuperacaoSenhaUseCase recuperar;
    private final DefinirSenhaPorTokenUseCase definirSenha;
    private final CookieRefresh cookie;

    public AuthResource(LoginUseCase login, RenovarSessaoUseCase renovar, EncerrarSessaoUseCase encerrar,
                        SolicitarRecuperacaoSenhaUseCase recuperar, DefinirSenhaPorTokenUseCase definirSenha,
                        CookieRefresh cookie) {
        this.login = login;
        this.renovar = renovar;
        this.encerrar = encerrar;
        this.recuperar = recuperar;
        this.definirSenha = definirSenha;
        this.cookie = cookie;
    }

    @POST
    @Path("/login")
    @PermitAll
    @Operation(summary = "Login por e-mail e senha", description = "Access token no corpo; refresh em cookie HttpOnly.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Sessão aberta."),
            @APIResponse(responseCode = "401", description = "E-mail ou senha inválidos, ou acesso indisponível."),
            @APIResponse(responseCode = "429", description = "Limite de requisições excedido.")
    })
    public Response login(LoginCommand cmd) {
        return sessao(Respostas.valor(login.execute(cmd)));
    }

    @POST
    @Path("/sessao/renovar")
    @Consumes(MediaType.WILDCARD)
    @PermitAll
    @OrigemPermitida
    @Operation(summary = "Renovar sessão", description = "Rotaciona o refresh token (cookie) e emite novo access token.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Sessão renovada."),
            @APIResponse(responseCode = "401", description = "Refresh inválido, expirado ou reutilizado."),
            @APIResponse(responseCode = "403", description = "Origin ausente ou não permitida."),
            @APIResponse(responseCode = "429", description = "Limite de requisições excedido.")
    })
    public Response renovar(@CookieParam(CookieRefresh.NOME) String refreshToken) {
        return sessao(Respostas.valor(renovar.execute(new RenovarSessaoCommand(refreshToken))));
    }

    @POST
    @Path("/sessao/logout")
    @Consumes(MediaType.WILDCARD)
    @PermitAll
    @OrigemPermitida
    @Operation(summary = "Encerrar sessão", description = "Idempotente: revoga a família do refresh e limpa o cookie.")
    @APIResponses({
            @APIResponse(responseCode = "204", description = "Sessão encerrada."),
            @APIResponse(responseCode = "403", description = "Origin ausente ou não permitida.")
    })
    public Response logout(@CookieParam(CookieRefresh.NOME) String refreshToken) {
        Respostas.valor(encerrar.execute(new EncerrarSessaoCommand(refreshToken)));
        return Response.noContent().cookie(cookie.limpar()).build();
    }

    @POST
    @Path("/senha/recuperar")
    @PermitAll
    @Operation(summary = "Recuperar senha", description = "Resposta sempre igual, exista ou não a conta.")
    @APIResponses({
            @APIResponse(responseCode = "202", description = "Solicitação recebida."),
            @APIResponse(responseCode = "429", description = "Limite de requisições excedido.")
    })
    public Response recuperar(SolicitarRecuperacaoSenhaCommand cmd) {
        return Response.accepted(Respostas.valor(recuperar.execute(cmd))).build();
    }

    @POST
    @Path("/senha/definir")
    @PermitAll
    @Operation(summary = "Definir senha por token", description = "Conclui SETUP ou RESET. Não abre sessão.")
    @APIResponses({
            @APIResponse(responseCode = "204", description = "Senha definida."),
            @APIResponse(responseCode = "400", description = "Token inválido ou senha fora da regra.")
    })
    public Response definirSenha(DefinirSenhaCommand cmd) {
        Respostas.valor(definirSenha.execute(cmd));
        return Response.noContent().build();
    }

    private Response sessao(SessaoEmitida s) {
        return Response.ok(SessaoDto.de(s.accessToken(), s.expiraEmSegundos()))
                .cookie(cookie.criar(s.refreshTokenPlano())).build();
    }
}
