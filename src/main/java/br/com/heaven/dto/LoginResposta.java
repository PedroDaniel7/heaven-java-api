package br.com.heaven.dto;

import java.time.OffsetDateTime;

/** Resposta de POST /api/auth/login. */
public record LoginResposta(String token, OffsetDateTime expiraEm, UsuarioDto usuario) {

    /** Nunca imprime o token. */
    @Override
    public String toString() {
        return "LoginResposta[expiraEm=" + expiraEm + "]";
    }
}
