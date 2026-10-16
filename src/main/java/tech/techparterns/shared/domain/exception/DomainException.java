package tech.techparterns.shared.domain.exception;

import java.util.List;

/**
 * Usada apenas quando um Result.fail chega a um ponto onde
 * não pode ser propagado (ex: reconstitution de dados do banco).
 */
public class DomainException extends RuntimeException {
    private final List<String> errors;

    public DomainException(List<String> errors) {
        super("Domain error: " + errors);
        this.errors = List.copyOf(errors);
    }

    public DomainException(String error) {
        this(List.of(error));
    }

    public DomainException(String error, Throwable cause) {
        super("Domain error: " + error, cause);
        this.errors = List.of(error);
    }

    public List<String> getErrors() { return errors; }
}