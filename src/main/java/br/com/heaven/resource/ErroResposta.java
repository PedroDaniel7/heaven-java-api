package br.com.heaven.resource;

import java.util.List;

/** Corpo de todo erro da API (contrato-api.md). */
public record ErroResposta(int status, String codigo, String mensagem, List<Campo> campos, String dataHora) {

    /** Erro de um campo, presente só em erro de validação. */
    public record Campo(String campo, String mensagem) {
    }
}
