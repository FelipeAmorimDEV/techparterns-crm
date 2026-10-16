package tech.techparterns.lead.application.csv;

import java.util.ArrayList;

import tech.techparterns.lead.domain.model.EmailContato;
import tech.techparterns.lead.domain.model.Lead;
import tech.techparterns.lead.domain.model.NomeContato;
import tech.techparterns.lead.domain.model.TelefoneBrasil;
import tech.techparterns.shared.domain.Result;

public final class ValidadorLinhaLead {

    /** Telefone, WhatsApp e e-mail são null quando ausentes. */
    public record LinhaValida(NomeContato nome, TelefoneBrasil telefone, TelefoneBrasil whatsapp, EmailContato email) {
    }

    private ValidadorLinhaLead() {}

    /** Em falha, {@code getErrors()} traz os motivos na ordem de RF27. */
    public static Result<LinhaValida> validar(LinhaCsv linha) {
        var motivos = new ArrayList<String>();

        var nome = NomeContato.tryCreate(linha.nomeContato());
        if (nome.isFailure())
            motivos.addAll(nome.getErrors());

        TelefoneBrasil telefone = null;
        if (presente(linha.telefone())) {
            var r = TelefoneBrasil.tryCreate(linha.telefone());
            if (r.isOk()) telefone = r.getValue(); else motivos.addAll(r.getErrors());
        }

        TelefoneBrasil whatsapp = null;
        if (presente(linha.whatsapp())) {
            var r = TelefoneBrasil.tryCreateWhatsapp(linha.whatsapp());
            if (r.isOk()) whatsapp = r.getValue(); else motivos.addAll(r.getErrors());
        }

        EmailContato email = null;
        if (presente(linha.email())) {
            var r = EmailContato.tryCreate(linha.email());
            if (r.isOk()) email = r.getValue();
        }

        if (telefone == null && whatsapp == null && email == null)
            motivos.add(Lead.SEM_MEIO);

        if (!motivos.isEmpty())
            return Result.fail(motivos);
        return Result.ok(new LinhaValida(nome.getValue(), telefone, whatsapp, email));
    }

    private static boolean presente(String valor) {
        return valor != null && !valor.trim().isEmpty();
    }
}
