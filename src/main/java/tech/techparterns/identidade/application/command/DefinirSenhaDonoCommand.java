package tech.techparterns.identidade.application.command;

import java.util.UUID;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Definição da senha do dono (comando de linha).")
public record DefinirSenhaDonoCommand(
        String senha) {
}
