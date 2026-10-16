package tech.techparterns.shared.infrastructure.rest;

import java.time.OffsetDateTime;

import br.com.felipeamorimdev.apontamento.domain.exception.ApontamentoDiaNaoEncontradoException;
import br.com.felipeamorimdev.apontamento.domain.exception.AtividadeCampoEmUsoException;
import br.com.felipeamorimdev.apontamento.domain.exception.AtividadeCampoNaoEncontradaException;
import br.com.felipeamorimdev.apontamento.domain.exception.UnidadeContagemNaoEncontradaException;
import br.com.felipeamorimdev.assistente.domain.exception.AssistenteIndisponivelException;
import br.com.felipeamorimdev.comercial.domain.exception.ClassificacaoNaoEncontradaException;
import br.com.felipeamorimdev.comercial.domain.exception.DestinatarioNaoEncontradoException;
import br.com.felipeamorimdev.comercial.domain.exception.PalletJaVendidoException;
import br.com.felipeamorimdev.comercial.domain.exception.VendaJaAcertadaException;
import br.com.felipeamorimdev.comercial.domain.exception.VendaNaoEncontradaException;
import br.com.felipeamorimdev.empreitas.domain.exception.ApontamentoEmpreitaDiaNaoEncontradoException;
import br.com.felipeamorimdev.empreitas.domain.exception.EmpreitaCicloEncerradoException;
import br.com.felipeamorimdev.empreitas.domain.exception.EmpreitaEmUsoException;
import br.com.felipeamorimdev.empreitas.domain.exception.EmpreitaNaoEncontradaException;
import br.com.felipeamorimdev.equipe.domain.exception.CargoCatalogoNaoEncontradoException;
import br.com.felipeamorimdev.equipe.domain.exception.CargoEmUsoException;
import br.com.felipeamorimdev.equipe.domain.exception.CargoNomeDuplicadoException;
import br.com.felipeamorimdev.equipe.domain.exception.FuncionarioNaoEncontradoException;
import br.com.felipeamorimdev.estoque.domain.exception.EstoqueInicialJaConcluidoException;
import br.com.felipeamorimdev.estoque.domain.exception.EstoqueInsuficienteException;
import br.com.felipeamorimdev.estoque.domain.exception.FornecedorNaoEncontradoException;
import br.com.felipeamorimdev.estoque.domain.exception.ItemNotaFiscalInvalidoException;
import br.com.felipeamorimdev.estoque.domain.exception.NotaFiscalJaPagaException;
import br.com.felipeamorimdev.estoque.domain.exception.NotaFiscalNaoEncontradaException;
import br.com.felipeamorimdev.estoque.domain.exception.NotaFiscalSemItensException;
import br.com.felipeamorimdev.estoque.domain.exception.ParcelaJaPagaException;
import br.com.felipeamorimdev.estoque.domain.exception.ParcelaNaoEncontradaException;
import br.com.felipeamorimdev.estoque.domain.exception.ProdutoDeOutraFazendaException;
import br.com.felipeamorimdev.estoque.domain.exception.ProdutoNaoEncontradoException;
import br.com.felipeamorimdev.estoque.domain.exception.RecursoNaoEncontradoException;
import br.com.felipeamorimdev.fitossanitario.domain.exception.AplicacaoNaoEncontradaException;
import br.com.felipeamorimdev.fitossanitario.domain.exception.FertirrigacaoNaoEncontradaException;
import br.com.felipeamorimdev.fitossanitario.domain.exception.IrrigacaoNaoEncontradaException;
import br.com.felipeamorimdev.fitossanitario.domain.exception.LancamentoStatusInvalidoException;
import br.com.felipeamorimdev.identidade.domain.exception.CredenciaisInvalidasException;
import br.com.felipeamorimdev.identidade.domain.exception.EmailJaCadastradoException;
import br.com.felipeamorimdev.identidade.domain.exception.FazendaNaoEncontradaException;
import br.com.felipeamorimdev.identidade.domain.exception.RefreshTokenInvalidoException;
import br.com.felipeamorimdev.identidade.domain.exception.UltimoAdminException;
import br.com.felipeamorimdev.producao.domain.exception.EmbalagemCatalogoNaoEncontradaException;
import br.com.felipeamorimdev.producao.domain.exception.EmbalagemCodigoDuplicadoException;
import br.com.felipeamorimdev.producao.domain.exception.FaseFenologicaCatalogoNaoEncontradaException;
import br.com.felipeamorimdev.producao.domain.exception.FaseFenologicaIntervaloSobrepostoException;
import br.com.felipeamorimdev.identidade.domain.exception.UsuarioNaoEncontradoException;
import br.com.felipeamorimdev.producao.domain.exception.CicloEmManejoDuplicadoException;
import br.com.felipeamorimdev.producao.domain.exception.CicloJaEmColheitaException;
import br.com.felipeamorimdev.producao.domain.exception.CicloJaEmManejoException;
import br.com.felipeamorimdev.producao.domain.exception.CicloNaoEstaEmColheitaException;
import br.com.felipeamorimdev.producao.domain.exception.CicloPlanejadoDuplicadoException;
import br.com.felipeamorimdev.producao.domain.exception.CicloProdutivoJaFinalizadoException;
import br.com.felipeamorimdev.producao.domain.exception.CicloProdutivoNaoEncontradoException;
import br.com.felipeamorimdev.producao.domain.exception.OcorrenciaAtividadeCicloCanceladaException;
import br.com.felipeamorimdev.producao.domain.exception.OcorrenciaAtividadeCicloNaoEncontradaException;
import br.com.felipeamorimdev.producao.domain.exception.CulturaNaoEncontradaException;
import br.com.felipeamorimdev.producao.domain.exception.ReceitaAtividadeCatalogoNaoEncontradaException;
import br.com.felipeamorimdev.producao.domain.exception.SetorNaoEncontradoException;
import br.com.felipeamorimdev.producao.domain.exception.VariedadeNaoEncontradaException;
import br.com.felipeamorimdev.rastreabilidade.domain.exception.CaixaJaVinculadaException;
import br.com.felipeamorimdev.rastreabilidade.domain.exception.CaixaNaoEncontradaException;
import br.com.felipeamorimdev.rastreabilidade.domain.exception.ColheitaJaFinalizadaException;
import br.com.felipeamorimdev.rastreabilidade.domain.exception.ColheitaNaoEncontradaException;
import br.com.felipeamorimdev.rastreabilidade.domain.exception.EmbalagemNaoEncontradaException;
import br.com.felipeamorimdev.rastreabilidade.domain.exception.PalletJaAdicionadoException;
import br.com.felipeamorimdev.rastreabilidade.domain.exception.PalletNaoEncontradoException;
import br.com.felipeamorimdev.rastreabilidade.domain.exception.QRCodeGenerationException;
import br.com.felipeamorimdev.solo.domain.exception.AnaliseSoloNaoEncontradaException;
import br.com.felipeamorimdev.solo.domain.exception.CicloEncerradoException;
import br.com.felipeamorimdev.solo.domain.exception.CicloNaoEncontradoException;
import br.com.felipeamorimdev.solo.domain.exception.ParametroSoloCodigoDuplicadoException;
import br.com.felipeamorimdev.solo.domain.exception.ParametroSoloNaoEncontradoException;
import tech.techparterns.shared.domain.exception.CulturaNaoHabilitadaException;
import tech.techparterns.shared.domain.exception.CulturaObrigatoriaException;
import tech.techparterns.shared.domain.exception.DomainException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.annotation.Priority;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class GlobalExceptionHandler implements ExceptionMapper<Throwable> {

    private static final String ERRO_INTERNO_MENSAGEM = "Erro interno no servidor";

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(Throwable exception) {
        var erroMapeado = mapear(exception);

        return Response.status(erroMapeado.status())
                .type(MediaType.APPLICATION_JSON)
                .entity(new ErroResponse(
                        path(),
                        erroMapeado.mensagem(),
                        erroMapeado.codigoErro(),
                        OffsetDateTime.now()))
                .build();
    }

    private ErroMapeado mapear(Throwable exception) {
        if (exception instanceof NotFoundException) {
            return new ErroMapeado(Response.Status.NOT_FOUND, "Rota não encontrada", "ROTA_NAO_ENCONTRADA");
        }
        if (exception instanceof ProdutoNaoEncontradoException) {
            return new ErroMapeado(Response.Status.NOT_FOUND, exception.getMessage(), "PRODUTO_NAO_ENCONTRADO");
        }
        if (exception instanceof EstoqueInsuficienteException) {
            return new ErroMapeado(Response.Status.CONFLICT, exception.getMessage(), "ESTOQUE_INSUFICIENTE");
        }
        if (exception instanceof EstoqueInicialJaConcluidoException) {
            return new ErroMapeado(Response.Status.CONFLICT, exception.getMessage(), "ESTOQUE_INICIAL_JA_CONCLUIDO");
        }
        if (exception instanceof NotaFiscalNaoEncontradaException) {
            return new ErroMapeado(Response.Status.NOT_FOUND, exception.getMessage(), "NOTA_FISCAL_NAO_ENCONTRADA");
        }
        if (exception instanceof NotaFiscalJaPagaException) {
            return new ErroMapeado(Response.Status.CONFLICT, exception.getMessage(), "NOTA_FISCAL_JA_PAGA");
        }
        if (exception instanceof ParcelaJaPagaException) {
            return new ErroMapeado(Response.Status.CONFLICT, exception.getMessage(), "PARCELA_JA_PAGA");
        }
        if (exception instanceof ParcelaNaoEncontradaException) {
            return new ErroMapeado(Response.Status.NOT_FOUND, exception.getMessage(), "PARCELA_NAO_ENCONTRADA");
        }
        if (exception instanceof NotaFiscalSemItensException || exception instanceof ItemNotaFiscalInvalidoException) {
            return new ErroMapeado(Response.Status.BAD_REQUEST, exception.getMessage(), "NOTA_FISCAL_INVALIDA");
        }
        if (exception instanceof ProdutoDeOutraFazendaException) {
            return new ErroMapeado(Response.Status.BAD_REQUEST, exception.getMessage(), "PRODUTO_FAZENDA_INVALIDA");
        }
        if (exception instanceof FornecedorNaoEncontradoException) {
            return new ErroMapeado(Response.Status.NOT_FOUND, exception.getMessage(), "FORNECEDOR_NAO_ENCONTRADO");
        }
        if (exception instanceof DestinatarioNaoEncontradoException) {
            return new ErroMapeado(Response.Status.NOT_FOUND, exception.getMessage(), "DESTINATARIO_NAO_ENCONTRADO");
        }
        if (exception instanceof ClassificacaoNaoEncontradaException) {
            return new ErroMapeado(Response.Status.NOT_FOUND, exception.getMessage(), "CLASSIFICACAO_NAO_ENCONTRADA");
        }
        if (exception instanceof FuncionarioNaoEncontradoException) {
            return new ErroMapeado(Response.Status.NOT_FOUND, exception.getMessage(), "FUNCIONARIO_NAO_ENCONTRADO");
        }
        if (exception instanceof VendaNaoEncontradaException) {
            return new ErroMapeado(Response.Status.NOT_FOUND, exception.getMessage(), "VENDA_NAO_ENCONTRADA");
        }
        if (exception instanceof VendaJaAcertadaException) {
            return new ErroMapeado(Response.Status.CONFLICT, exception.getMessage(), "VENDA_JA_ACERTADA");
        }
        if (exception instanceof PalletJaVendidoException) {
            return new ErroMapeado(Response.Status.CONFLICT, exception.getMessage(), "PALLET_JA_VENDIDO");
        }
        if (exception instanceof RecursoNaoEncontradoException) {
            return new ErroMapeado(Response.Status.NOT_FOUND, exception.getMessage(), "RECURSO_NAO_ENCONTRADO");
        }
        if (exception instanceof CicloProdutivoNaoEncontradoException) {
            return new ErroMapeado(Response.Status.NOT_FOUND, exception.getMessage(), "CICLO_NAO_ENCONTRADO");
        }
        if (exception instanceof VariedadeNaoEncontradaException) {
            return new ErroMapeado(Response.Status.NOT_FOUND, exception.getMessage(), "VARIEDADE_NAO_ENCONTRADA");
        }
        if (exception instanceof CulturaNaoEncontradaException) {
            return new ErroMapeado(Response.Status.BAD_REQUEST, exception.getMessage(), "CULTURA_NAO_ENCONTRADA");
        }
        if (exception instanceof SetorNaoEncontradoException) {
            return new ErroMapeado(Response.Status.NOT_FOUND, exception.getMessage(), "SETOR_NAO_ENCONTRADO");
        }
        if (exception instanceof CicloProdutivoJaFinalizadoException) {
            return new ErroMapeado(Response.Status.CONFLICT, exception.getMessage(), "CICLO_JA_FINALIZADO");
        }
        if (exception instanceof OcorrenciaAtividadeCicloCanceladaException) {
            return new ErroMapeado(Response.Status.CONFLICT, exception.getMessage(), "OCORRENCIA_CANCELADA");
        }
        if (exception instanceof OcorrenciaAtividadeCicloNaoEncontradaException) {
            return new ErroMapeado(Response.Status.NOT_FOUND, exception.getMessage(), "OCORRENCIA_NAO_ENCONTRADA");
        }
        if (exception instanceof CicloJaEmColheitaException) {
            return new ErroMapeado(Response.Status.CONFLICT, exception.getMessage(), "CICLO_JA_EM_COLHEITA");
        }
        if (exception instanceof CicloJaEmManejoException) {
            return new ErroMapeado(Response.Status.CONFLICT, exception.getMessage(), "CICLO_JA_EM_MANEJO");
        }
        if (exception instanceof CicloPlanejadoDuplicadoException) {
            return new ErroMapeado(Response.Status.CONFLICT, exception.getMessage(), "CICLO_PLANEJADO_DUPLICADO");
        }
        if (exception instanceof CicloEmManejoDuplicadoException) {
            return new ErroMapeado(Response.Status.CONFLICT, exception.getMessage(), "CICLO_EM_MANEJO_DUPLICADO");
        }
        if (exception instanceof CicloNaoEstaEmColheitaException) {
            return new ErroMapeado(Response.Status.CONFLICT, exception.getMessage(), "CICLO_NAO_ESTA_EM_COLHEITA");
        }
        if (exception instanceof ColheitaNaoEncontradaException) {
            return new ErroMapeado(Response.Status.NOT_FOUND, exception.getMessage(), "COLHEITA_NAO_ENCONTRADA");
        }
        if (exception instanceof EmbalagemNaoEncontradaException) {
            return new ErroMapeado(Response.Status.NOT_FOUND, exception.getMessage(), "EMBALAGEM_NAO_ENCONTRADA");
        }
        if (exception instanceof PalletNaoEncontradoException) {
            return new ErroMapeado(Response.Status.NOT_FOUND, exception.getMessage(), "PALLET_NAO_ENCONTRADO");
        }
        if (exception instanceof CaixaNaoEncontradaException) {
            return new ErroMapeado(Response.Status.NOT_FOUND, exception.getMessage(), "CAIXA_NAO_ENCONTRADA");
        }
        if (exception instanceof CaixaJaVinculadaException) {
            return new ErroMapeado(Response.Status.CONFLICT, exception.getMessage(), "CAIXA_JA_VINCULADA");
        }
        if (exception instanceof ColheitaJaFinalizadaException) {
            return new ErroMapeado(Response.Status.CONFLICT, exception.getMessage(), "COLHEITA_JA_FINALIZADA");
        }
        if (exception instanceof PalletJaAdicionadoException) {
            return new ErroMapeado(Response.Status.CONFLICT, exception.getMessage(), "PALLET_JA_ADICIONADO");
        }
        if (exception instanceof QRCodeGenerationException) {
            return new ErroMapeado(Response.Status.INTERNAL_SERVER_ERROR, exception.getMessage(), "ERRO_GERACAO_QRCODE");
        }
        if (exception instanceof LancamentoStatusInvalidoException lsie) {
            String mensagem = lsie.getErrors().size() == 1
                    ? lsie.getErrors().get(0)
                    : String.join("; ", lsie.getErrors());
            return new ErroMapeado(Response.Status.CONFLICT, mensagem, "LANCAMENTO_STATUS_INVALIDO");
        }
        if (exception instanceof AplicacaoNaoEncontradaException) {
            return new ErroMapeado(Response.Status.NOT_FOUND, exception.getMessage(), "APLICACAO_NAO_ENCONTRADA");
        }
        if (exception instanceof FertirrigacaoNaoEncontradaException) {
            return new ErroMapeado(Response.Status.NOT_FOUND, exception.getMessage(), "FERTIRRIGACAO_NAO_ENCONTRADA");
        }
        if (exception instanceof IrrigacaoNaoEncontradaException) {
            return new ErroMapeado(Response.Status.NOT_FOUND, exception.getMessage(), "IRRIGACAO_NAO_ENCONTRADA");
        }
        if (exception instanceof EmailJaCadastradoException) {
            return new ErroMapeado(Response.Status.CONFLICT, exception.getMessage(), "EMAIL_JA_CADASTRADO");
        }
        if (exception instanceof CredenciaisInvalidasException) {
            return new ErroMapeado(Response.Status.UNAUTHORIZED, exception.getMessage(), "CREDENCIAIS_INVALIDAS");
        }
        if (exception instanceof RefreshTokenInvalidoException) {
            return new ErroMapeado(Response.Status.UNAUTHORIZED, exception.getMessage(), "REFRESH_TOKEN_INVALIDO");
        }
        if (exception instanceof UsuarioNaoEncontradoException) {
            return new ErroMapeado(Response.Status.NOT_FOUND, exception.getMessage(), "USUARIO_NAO_ENCONTRADO");
        }
        if (exception instanceof FazendaNaoEncontradaException) {
            return new ErroMapeado(Response.Status.NOT_FOUND, exception.getMessage(), "FAZENDA_NAO_ENCONTRADA");
        }
        if (exception instanceof UltimoAdminException) {
            return new ErroMapeado(Response.Status.CONFLICT, exception.getMessage(), "ULTIMO_ADMIN");
        }
        if (exception instanceof EmbalagemCatalogoNaoEncontradaException) {
            return new ErroMapeado(Response.Status.NOT_FOUND, exception.getMessage(), "EMBALAGEM_CATALOGO_NAO_ENCONTRADA");
        }
        if (exception instanceof EmbalagemCodigoDuplicadoException) {
            return new ErroMapeado(Response.Status.CONFLICT, exception.getMessage(), "EMBALAGEM_CODIGO_DUPLICADO");
        }
        if (exception instanceof FaseFenologicaCatalogoNaoEncontradaException) {
            return new ErroMapeado(Response.Status.NOT_FOUND, exception.getMessage(), "FASE_FENOLOGICA_NAO_ENCONTRADA");
        }
        if (exception instanceof FaseFenologicaIntervaloSobrepostoException) {
            return new ErroMapeado(Response.Status.CONFLICT, exception.getMessage(), "FASE_FENOLOGICA_INTERVALO_SOBREPOSTO");
        }
        if (exception instanceof ReceitaAtividadeCatalogoNaoEncontradaException) {
            return new ErroMapeado(Response.Status.NOT_FOUND, exception.getMessage(), "RECEITA_ATIVIDADE_NAO_ENCONTRADA");
        }
        if (exception instanceof CulturaObrigatoriaException) {
            return new ErroMapeado(Response.Status.BAD_REQUEST, exception.getMessage(), "CULTURA_OBRIGATORIA");
        }
        if (exception instanceof CulturaNaoHabilitadaException) {
            return new ErroMapeado(Response.Status.BAD_REQUEST, exception.getMessage(), "CULTURA_NAO_HABILITADA");
        }
        if (exception instanceof AssistenteIndisponivelException) {
            return new ErroMapeado(Response.Status.BAD_GATEWAY, "Assistente indisponível", "ASSISTENTE_INDISPONIVEL");
        }
        if (exception instanceof AtividadeCampoNaoEncontradaException) {
            return new ErroMapeado(Response.Status.NOT_FOUND, exception.getMessage(), "ATIVIDADE_CAMPO_NAO_ENCONTRADA");
        }
        if (exception instanceof UnidadeContagemNaoEncontradaException) {
            return new ErroMapeado(Response.Status.NOT_FOUND, exception.getMessage(), "UNIDADE_CONTAGEM_NAO_ENCONTRADA");
        }
        if (exception instanceof ApontamentoDiaNaoEncontradoException) {
            return new ErroMapeado(Response.Status.NOT_FOUND, exception.getMessage(), "APONTAMENTO_DIA_NAO_ENCONTRADO");
        }
        if (exception instanceof AtividadeCampoEmUsoException) {
            return new ErroMapeado(Response.Status.CONFLICT, exception.getMessage(), "ATIVIDADE_CAMPO_EM_USO");
        }
        if (exception instanceof EmpreitaNaoEncontradaException) {
            return new ErroMapeado(Response.Status.NOT_FOUND, exception.getMessage(), "EMPREITA_NAO_ENCONTRADA");
        }
        if (exception instanceof EmpreitaEmUsoException) {
            return new ErroMapeado(Response.Status.CONFLICT, exception.getMessage(), "EMPREITA_EM_USO");
        }
        if (exception instanceof ApontamentoEmpreitaDiaNaoEncontradoException) {
            return new ErroMapeado(Response.Status.NOT_FOUND, exception.getMessage(),
                    "APONTAMENTO_EMPREITA_DIA_NAO_ENCONTRADO");
        }
        if (exception instanceof EmpreitaCicloEncerradoException) {
            return new ErroMapeado(Response.Status.CONFLICT, exception.getMessage(), "CICLO_ENCERRADO");
        }
        if (exception instanceof CargoCatalogoNaoEncontradoException) {
            return new ErroMapeado(Response.Status.NOT_FOUND, exception.getMessage(), "CARGO_NAO_ENCONTRADO");
        }
        if (exception instanceof CargoEmUsoException) {
            return new ErroMapeado(Response.Status.CONFLICT, exception.getMessage(), "CARGO_EM_USO");
        }
        if (exception instanceof CargoNomeDuplicadoException) {
            return new ErroMapeado(Response.Status.CONFLICT, exception.getMessage(), "CARGO_NOME_DUPLICADO");
        }
        if (exception instanceof ParametroSoloCodigoDuplicadoException) {
            return new ErroMapeado(Response.Status.CONFLICT, exception.getMessage(), "PARAMETRO_SOLO_CODIGO_DUPLICADO");
        }
        if (exception instanceof ParametroSoloNaoEncontradoException) {
            return new ErroMapeado(Response.Status.NOT_FOUND, exception.getMessage(), "PARAMETRO_SOLO_NAO_ENCONTRADO");
        }
        if (exception instanceof CicloNaoEncontradoException) {
            return new ErroMapeado(Response.Status.NOT_FOUND, exception.getMessage(), "CICLO_NAO_ENCONTRADO");
        }
        if (exception instanceof CicloEncerradoException) {
            return new ErroMapeado(Response.Status.BAD_REQUEST, exception.getMessage(), "CICLO_ENCERRADO");
        }
        if (exception instanceof AnaliseSoloNaoEncontradaException) {
            return new ErroMapeado(Response.Status.NOT_FOUND, exception.getMessage(), "ANALISE_SOLO_NAO_ENCONTRADA");
        }
        if (exception instanceof DomainException de) {
            String mensagem = de.getErrors().size() == 1
                    ? de.getErrors().get(0)
                    : String.join("; ", de.getErrors());
            return new ErroMapeado(Response.Status.BAD_REQUEST, mensagem, "REQUISICAO_INVALIDA");
        }
        if (exception instanceof IllegalArgumentException) {
            return new ErroMapeado(Response.Status.BAD_REQUEST, exception.getMessage(), "REQUISICAO_INVALIDA");
        }
        if (exception instanceof IllegalStateException
                && exception.getMessage() != null
                && exception.getMessage().contains("Tenant")) {
            return new ErroMapeado(Response.Status.UNAUTHORIZED, exception.getMessage(), "TENANT_AUSENTE");
        }
        var causa = exception;
        while (causa != null) {
            if (causa instanceof InvalidFormatException) {
                return new ErroMapeado(Response.Status.BAD_REQUEST, "Requisição inválida", "REQUISICAO_INVALIDA");
            }
            causa = causa.getCause();
        }
        return new ErroMapeado(Response.Status.INTERNAL_SERVER_ERROR, ERRO_INTERNO_MENSAGEM, "ERRO_INTERNO");
    }

    private String path() {
        if (uriInfo == null) {
            return "/";
        }
        String path = uriInfo.getPath();
        return path.startsWith("/") ? path : "/" + path;
    }

    private record ErroMapeado(Response.Status status, String mensagem, String codigoErro) {
    }
}

@Provider
@Priority(Priorities.USER)
class InvalidFormatExceptionHandler implements ExceptionMapper<InvalidFormatException> {

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(InvalidFormatException exception) {
        String path = "/";
        if (uriInfo != null) {
            String raw = uriInfo.getPath();
            path = raw.startsWith("/") ? raw : "/" + raw;
        }
        return Response.status(Response.Status.BAD_REQUEST)
                .type(MediaType.APPLICATION_JSON)
                .entity(new ErroResponse(path, "Requisição inválida", "REQUISICAO_INVALIDA", OffsetDateTime.now()))
                .build();
    }
}
