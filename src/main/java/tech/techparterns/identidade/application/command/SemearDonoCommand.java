package tech.techparterns.identidade.application.command;

import java.util.UUID;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Semente do primeiro dono da plataforma.")
public record SemearDonoCommand(
        String email,
        String nome) {
}
