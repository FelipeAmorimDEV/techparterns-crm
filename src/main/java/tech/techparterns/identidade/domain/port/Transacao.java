package tech.techparterns.identidade.domain.port;

import java.util.function.Supplier;

/**
 * Executa uma ação em transação própria e traduz violação de unicidade em {@link ViolacaoUnicidadeException},
 * para o chamador (fora da transação) poder responder de forma neutra (CA-11).
 */
public interface Transacao {

    <T> T emNovaTransacao(Supplier<T> acao);

    class ViolacaoUnicidadeException extends RuntimeException {
        public ViolacaoUnicidadeException(Throwable causa) { super("Violação de unicidade", causa); }
    }
}
