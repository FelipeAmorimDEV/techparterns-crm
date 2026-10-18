package tech.techparterns.identidade.infrastructure.persistence;

import java.sql.SQLException;
import java.util.function.Supplier;

import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.enterprise.context.ApplicationScoped;

import tech.techparterns.identidade.domain.port.Transacao;

@ApplicationScoped
public class TransacaoImpl implements Transacao {

    @Override
    public <T> T emNovaTransacao(Supplier<T> acao) {
        try {
            return QuarkusTransaction.requiringNew().call(acao::get);
        } catch (RuntimeException e) {
            if (violacaoDeUnicidade(e))
                throw new ViolacaoUnicidadeException(e);
            throw e;
        }
    }

    static boolean violacaoDeUnicidade(Throwable erro) {
        for (Throwable t = erro; t != null; t = t.getCause()) {
            if (t instanceof SQLException sql && "23505".equals(sql.getSQLState()))
                return true;
        }
        return false;
    }
}
