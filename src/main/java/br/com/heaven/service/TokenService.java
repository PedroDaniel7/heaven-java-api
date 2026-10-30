package br.com.heaven.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Base64;

/**
 * Token de sessão assinado com HMAC-SHA256, no formato {@code base64url(idUsuario:expiraEmSegundos).base64url(assinatura)}.
 * O segredo vem de HEAVEN_TOKEN_SECRET; sem ele (ou com menos de 32 caracteres) a API não sobe.
 */
@Component
public class TokenService {

    public static final Duration VALIDADE = Duration.ofHours(12);
    private static final int TAMANHO_MINIMO_SEGREDO = 32;
    private static final String ALGORITMO = "HmacSHA256";
    private static final String MENSAGEM = "Token ausente, inválido ou expirado.";
    private static final Base64.Encoder CODIFICADOR = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODIFICADOR = Base64.getUrlDecoder();

    /** Token emitido e o instante em que deixa de valer. */
    public record TokenEmitido(String token, OffsetDateTime expiraEm) {
        @Override
        public String toString() {
            return "TokenEmitido[expiraEm=" + expiraEm + "]";
        }
    }

    private final byte[] segredo;
    private final Clock relogio;

    @Autowired
    public TokenService(@Value("${HEAVEN_TOKEN_SECRET:}") String segredo) {
        this(segredo, Clock.systemDefaultZone());
    }

    public TokenService(String segredo, Clock relogio) {
        if (segredo == null || segredo.strip().length() < TAMANHO_MINIMO_SEGREDO)
            throw new IllegalStateException("HEAVEN_TOKEN_SECRET ausente ou com menos de " + TAMANHO_MINIMO_SEGREDO + " caracteres.");
        this.segredo = segredo.strip().getBytes(StandardCharsets.UTF_8);
        this.relogio = relogio;
    }

    public TokenEmitido emitir(long idUsuario) {
        Instant expira = relogio.instant().plus(VALIDADE);
        String carga = CODIFICADOR.encodeToString((idUsuario + ":" + expira.getEpochSecond()).getBytes(StandardCharsets.UTF_8));
        String token = carga + "." + CODIFICADOR.encodeToString(assinar(carga));
        return new TokenEmitido(token, expira.atZone(relogio.getZone()).toOffsetDateTime());
    }

    /** Devolve o id do usuário; token malformado, adulterado ou expirado vira 401 NAO_AUTENTICADO. */
    public long validar(String token) {
        try {
            if (token == null) throw negado();
            int ponto = token.indexOf('.');
            if (ponto <= 0 || ponto != token.lastIndexOf('.')) throw negado();
            String carga = token.substring(0, ponto);
            String assinaturaTexto = token.substring(ponto + 1);
            byte[] recebida = DECODIFICADOR.decode(assinaturaTexto);
            // O último caractere do base64 tem bits sobrando: sem esta checagem, trocá-lo poderia decodificar para os mesmos bytes.
            if (!CODIFICADOR.encodeToString(recebida).equals(assinaturaTexto)) throw negado();
            if (!MessageDigest.isEqual(recebida, assinar(carga))) throw negado();
            String[] partes = new String(DECODIFICADOR.decode(carga), StandardCharsets.UTF_8).split(":");
            if (partes.length != 2) throw negado();
            long id = Long.parseLong(partes[0]);
            long expira = Long.parseLong(partes[1]);
            if (relogio.instant().getEpochSecond() >= expira) throw negado();
            return id;
        } catch (IllegalArgumentException e) {
            throw negado();
        }
    }

    private byte[] assinar(String carga) {
        try {
            Mac mac = Mac.getInstance(ALGORITMO);
            mac.init(new SecretKeySpec(segredo, ALGORITMO));
            return mac.doFinal(carga.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private static RegraNegocioException negado() {
        return new RegraNegocioException(CodigoErro.NAO_AUTENTICADO, MENSAGEM);
    }
}
