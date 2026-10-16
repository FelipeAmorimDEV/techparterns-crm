package tech.techparterns.distribuicao.infrastructure.rest;

import java.util.List;
import java.util.UUID;

import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import tech.techparterns.distribuicao.application.ConfiguracaoDistribuicao;
import tech.techparterns.distribuicao.application.Contratos;
import tech.techparterns.distribuicao.application.OperacoesDistribuicao;
import tech.techparterns.distribuicao.application.VerificarParados;
import tech.techparterns.shared.domain.exception.DomainException;

@Path("/distribuicao")
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class DistribuicaoResource {

    private final OperacoesDistribuicao operacoes;
    private final ConfiguracaoDistribuicao configuracao;
    private final VerificarParados parados;

    public DistribuicaoResource(OperacoesDistribuicao operacoes, ConfiguracaoDistribuicao configuracao,
                                VerificarParados parados) {
        this.operacoes = operacoes;
        this.configuracao = configuracao;
        this.parados = parados;
    }

    @GET
    @Path("/sem-dono")
    @RolesAllowed("ADMIN")
    public List<Contratos.LeadSemDono> semDono(@QueryParam("uf") String uf, @QueryParam("cidade") String cidade,
                                               @QueryParam("quente") Boolean quente, @QueryParam("origem") String origem) {
        return operacoes.listarSemDono(uf, cidade, quente, origem);
    }

    @POST
    @Path("/atribuicoes")
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed("ADMIN")
    public Contratos.AtribuicaoManual atribuir(Contratos.AtribuirManual cmd) {
        return valor(operacoes.atribuir(cmd));
    }

    @POST
    @Path("/lotes/simulacao")
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed("ADMIN")
    public Contratos.ResultadoLote simularLote(Contratos.Lote cmd) {
        return valor(operacoes.simularLote(cmd));
    }

    @POST
    @Path("/lotes/confirmacao")
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed("ADMIN")
    public Contratos.ResultadoLote confirmarLote(Contratos.Lote cmd) {
        return valor(operacoes.confirmarLote(cmd));
    }

    @POST
    @Path("/carteiras/{vendedorId}/simulacao")
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed("ADMIN")
    public Contratos.ResultadoLote simularCarteira(@PathParam("vendedorId") UUID vendedorId, Contratos.Carteira cmd) {
        return valor(operacoes.simularCarteira(vendedorId, cmd));
    }

    @POST
    @Path("/carteiras/{vendedorId}/confirmacao")
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed("ADMIN")
    public Contratos.ResultadoLote confirmarCarteira(@PathParam("vendedorId") UUID vendedorId, Contratos.Carteira cmd) {
        return valor(operacoes.confirmarCarteira(vendedorId, cmd));
    }

    @POST
    @Path("/fila/proximo")
    @RolesAllowed("SELLER")
    public Contratos.ProximoFila proximo() {
        return valor(operacoes.pegarFila());
    }

    @POST
    @Path("/fila/devolucoes")
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed("SELLER")
    public Response devolver(Contratos.Devolucao cmd) {
        valor(operacoes.devolver(cmd));
        return Response.noContent().build();
    }

    @GET
    @Path("/leads/{id}/historico")
    @RolesAllowed("ADMIN")
    public List<Contratos.LinhaHistorico> historico(@PathParam("id") UUID id) {
        return operacoes.historico(id);
    }

    @PUT
    @Path("/configuracao")
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed("ADMIN")
    public Response configuracao(Contratos.Configuracao cmd) {
        valor(configuracao.salvarEmpresa(cmd));
        return Response.noContent().build();
    }

    @PUT
    @Path("/vendedores/{id}/configuracao")
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed("ADMIN")
    public Response configVendedor(@PathParam("id") UUID id, Contratos.ConfigVendedorBody cmd) {
        valor(configuracao.salvarVendedor(id, cmd));
        return Response.noContent().build();
    }

    @POST
    @Path("/territorios")
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed("ADMIN")
    public Response territorio(Contratos.TerritorioBody cmd) {
        var id = valor(configuracao.criarTerritorio(cmd));
        return Response.status(Response.Status.CREATED).entity(id).build();
    }

    @POST
    @Path("/parados/verificacao")
    @RolesAllowed("ADMIN")
    public Contratos.VerificacaoParado verificarParados() {
        return valor(parados.executar());
    }

    private static <T> T valor(tech.techparterns.shared.domain.Result<T> resultado) {
        if (resultado.isFailure())
            throw new DomainException(resultado.getErrors());
        return resultado.getValue();
    }
}
