package br.com.heaven.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Chave das rotas de administração (HEAVEN_ADMIN_KEY). Sem a variável, ou com menos de 24 caracteres,
 * a API sobe normalmente e toda chamada admin é recusada. A comparação é em tempo constante.
 */
@Component
public class ChaveAdmin {

    private static final Logger LOG = LoggerFactory.getLogger(ChaveAdmin.class);
    public static final int TAMANHO_MINIMO = 24;

    private final byte[] resumo;

    public ChaveAdmin(@Value("${HEAVEN_ADMIN_KEY:}") String chave) {
        String limpa = chave == null ? "" : chave.strip();
        this.resumo = limpa.length() >= TAMANHO_MINIMO ? sha256(limpa) : null;
        LOG.info("Rotas de administração: {}", resumo != null ? "habilitadas" : "desabilitadas");
    }

    /** Falso quando a chave do servidor não está configurada ou a enviada é nula ou diferente. */
    public boolean confere(String enviada) {
        if (resumo == null || enviada == null) return false;
        // Compara os resumos SHA-256: o tempo não depende do tamanho nem do conteúdo da chave enviada.
        return MessageDigest.isEqual(resumo, sha256(enviada.strip()));
    }

    private static byte[] sha256(String texto) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
