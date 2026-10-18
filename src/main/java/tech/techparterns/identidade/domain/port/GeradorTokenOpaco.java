package tech.techparterns.identidade.domain.port;

import tech.techparterns.identidade.domain.model.HashToken;

public interface GeradorTokenOpaco {

    /** Token aleatório de 256 bits em Base64 URL, sem padding. */
    String gerar();

    HashToken hash(String tokenPlano);

    /** SHA-256 hexadecimal de um texto qualquer (usado para não guardar o e-mail digitado nos contadores de login). */
    String hashTexto(String texto);
}
