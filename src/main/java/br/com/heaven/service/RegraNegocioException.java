package br.com.heaven.service;

import java.util.Objects;

/** Violação de regra de negócio. O {@link CodigoErro} define o status HTTP da resposta. */
public class RegraNegocioException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final CodigoErro codigo;

    public RegraNegocioException(CodigoErro codigo, String mensagem) {
        super(mensagem);
        this.codigo = Objects.requireNonNull(codigo);
    }

    public CodigoErro getCodigo() {
        return codigo;
    }
}
