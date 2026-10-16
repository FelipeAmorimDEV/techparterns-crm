# Resource Pattern (techparterns-crm — Quarkus)

Pacote: `tech.techparterns.<modulo>.infrastructure.rest`.

Handler: `shared/infrastructure/rest/GlobalExceptionHandler.java`.
Corpo: `shared/infrastructure/rest/ErroResponse.java`.

## Checklist

- `@Path`, `@Produces`, `@Consumes`, `@ApplicationScoped`, `@Tag`.
- Construtor com os use cases.
- `@Operation` e `@APIResponses` em cada método.
- `@Parameter` em cada `@QueryParam`.
- `@RequestBody` no body, com `@Schema`.
- 201 criação, 204 sem corpo, 200 consulta.
- 400 documentado quando o fluxo pode devolver `Result` falho ou `DomainException`.

## POST com corpo

```java
@POST
@Operation(summary = "Criar exemplo", description = "Cria um exemplo na empresa do token.")
@APIResponses({
        @APIResponse(responseCode = "201", description = "Criado.",
                content = @Content(mediaType = MediaType.APPLICATION_JSON,
                        schema = @Schema(implementation = ExemploResponseDto.class))),
        @APIResponse(responseCode = "400", description = "Requisição inválida.")
})
public Response criar(
        @RequestBody(required = true,
                content = @Content(schema = @Schema(implementation = CriarExemploCommand.class)))
        CriarExemploCommand cmd) {
    var resultado = criarExemplo.execute(cmd);
    if (resultado.isFailure())
        throw new DomainException(resultado.getErrors());
    return Response.status(Response.Status.CREATED)
            .entity(new ExemploResponseDto(resultado.getValue()))
            .build();
}
```

`DomainException` e `ExemploResponseDto` usam os pacotes `tech.techparterns.shared.domain.exception` e `tech.techparterns.<modulo>.application.dto`.

## POST sem corpo de retorno

`execute`; em falha, `DomainException`; em sucesso, `Response.noContent().build()`.

## GET paginado

```java
@GET
public ExemploListaDto listar(
        @Parameter(description = "Filtro por nome.") @QueryParam("q") String q,
        @Parameter(description = "Página, começando em 0.")
        @QueryParam("page") @DefaultValue("0") int page,
        @Parameter(description = "Itens por página.")
        @QueryParam("perPage") @DefaultValue("20") int perPage) {
    return listar.execute(new ListarExemplosQuery(page, perPage, q)).getValue();
}
```

Sem query param de empresa.

## Classe

```java
@Path("/exemplos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Tag(name = "Exemplos", description = "Exemplos da empresa autenticada.")
public class ExemploResource {

    private final CriarExemploUseCase criarExemplo;
    private final ListarExemplosUseCase listar;

    public ExemploResource(CriarExemploUseCase criarExemplo, ListarExemplosUseCase listar) {
        this.criarExemplo = criarExemplo;
        this.listar = listar;
    }
}
```

## Armadilhas

- Validar nome vazio no resource. Isso é VO.
- Chamar `executar`.
- Ignorar `isFailure()` e chamar `getValue()` em comando: `getValue()` em falha lança `IllegalStateException` e o cliente vê 500.
- Capturar `DomainException` e montar o `Response` na mão.
- Documentar 404 ou 409 de domínio que o handler ainda não distingue.
- `@QueryParam("fazendaId")` ou `@QueryParam("companyId")`.
- Reativar o 401 de tenant comentado no handler como parte de um endpoint novo.
