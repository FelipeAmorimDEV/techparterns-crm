package tech.techparterns.identidade.infrastructure.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

import jakarta.enterprise.context.ApplicationScoped;

import tech.techparterns.identidade.domain.model.HashToken;
import tech.techparterns.identidade.domain.port.GeradorTokenOpaco;

@ApplicationScoped
public class GeradorTokenOpacoSha256 implements GeradorTokenOpaco {

    private final SecureRandom random = new SecureRandom();

    @Override
    public String gerar() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    @Override
    public HashToken hash(String tokenPlano) {
        return HashToken.reconstituir(sha256Hex(tokenPlano == null ? "" : tokenPlano));
    }

    @Override
    public String hashTexto(String texto) {
        return sha256Hex(texto);
    }

    public static String sha256Hex(String texto) {
        try {
            var md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(texto.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
