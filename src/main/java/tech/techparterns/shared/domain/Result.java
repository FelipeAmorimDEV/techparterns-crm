package tech.techparterns.shared.domain;


import java.util.Arrays;
import java.util.List;

import tech.techparterns.shared.domain.exception.DomainException;

public final class Result<T> {

    private final T value;
    private final List<String> errors;

    private Result(T value, List<String> errors) {
        this.value  = value;
        this.errors = errors != null ? List.copyOf(errors) : List.of();
    }

    // ── Fábricas ──────────────────────────────────────────────────────────

    public static <T> Result<T> ok(T value) {
        return new Result<>(value, List.of());
    }

    public static <T> Result<T> ok() {
        return new Result<>(null, List.of());
    }

    public static <T> Result<T> fail(String... errors) {
        return new Result<>(null, Arrays.asList(errors));
    }

    public static <T> Result<T> fail(List<String> errors) {
        return new Result<>(null, errors);
    }

    /**
     * Repassa os erros de um Result<A> para um Result<B>.
     * Equivale ao .withFail do TypeScript.
     */
    public static <B> Result<B> propagate(Result<?> source) {
        return Result.fail(source.getErrors());
    }

    /**
     * Executa fn() e captura qualquer Exception como Result.fail.
     * Equivale ao Result.try() do TypeScript.
     */
    public static <T> Result<T> attempt(ThrowingSupplier<T> fn) {
        try {
            return Result.ok(fn.get());
        } catch (Exception e) {
            return Result.fail(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
        }
    }

    // ── Combinadores ──────────────────────────────────────────────────────

    /**
     * Combina N Results. Se qualquer um falhar, retorna todos os erros juntos.
     * Equivale ao Result.combine() do TypeScript.
     */
    @SafeVarargs
    public static <T> Result<List<T>> combine(Result<T>... results) {
        var allErrors = Arrays.stream(results)
            .filter(Result::isFailure)
            .flatMap(r -> r.getErrors().stream())
            .toList();

        if (!allErrors.isEmpty())
            return Result.fail(allErrors);

        var values = Arrays.stream(results)
            .map(Result::getValue)
            .toList();

        return Result.ok(values);
    }

    /**
     * Versão sem tipo genérico para combinar Results de tipos diferentes.
     * Útil para validação de entidades (como Product.tryCreate).
     */
    public static Result<Void> combineAll(Result<?>... results) {
        var allErrors = Arrays.stream(results)
            .filter(Result::isFailure)
            .flatMap(r -> r.getErrors().stream())
            .toList();

        return allErrors.isEmpty() ? Result.ok() : Result.fail(allErrors);
    }

    // ── Consultas ─────────────────────────────────────────────────────────

    public boolean isOk()      { return errors.isEmpty(); }
    public boolean isFailure() { return !errors.isEmpty(); }

    public T getValue() {
        if (isFailure())
            throw new IllegalStateException("Tentativa de getValue() em Result.fail: " + errors);
        return value;
    }

    public List<String> getErrors() { return errors; }

    /**
     * Lança RuntimeException se falhou.
     * Útil em reconstitution de entidades (dados já persistidos, nunca inválidos).
     */
    public T getOrThrow() {
        if (isFailure())
            throw new DomainException(errors);
        return value;
    }

    @Override
    public String toString() {
        return isOk()
            ? "Result.ok(" + value + ")"
            : "Result.fail(" + errors + ")";
    }

    @FunctionalInterface
    public interface ThrowingSupplier<T> {
        T get() throws Exception;
    }
}