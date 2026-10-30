package br.com.heaven.service;

/** Códigos de erro do contrato da API (contrato-api.md) e o status HTTP de cada um. */
public enum CodigoErro {
    VALIDACAO(400),
    NAO_AUTENTICADO(401),
    PROIBIDO(403),
    NAO_ENCONTRADO(404),
    CONFLITO(409),
    REGRA(422),
    OFERTA_EXPIRADA(409),
    BANCO_INDISPONIVEL(503),
    ERRO_INTERNO(500);

    private final int status;

    CodigoErro(int status) {
        this.status = status;
    }

    public int getStatus() {
        return status;
    }

    /** Primeiro código com o status HTTP dado (409 devolve CONFLITO). */
    public static CodigoErro porStatus(int status) {
        for (CodigoErro c : values()) if (c.status == status) return c;
        throw new IllegalArgumentException("Status sem código de erro: " + status);
    }
}
