package br.com.heaven.bo;

import br.com.heaven.model.AcaoUsuario;

import java.time.Instant;

/** Ação sorteada para o validador e o instante em que a oferta expira. */
public record Sorteio(AcaoUsuario acao, Instant expiraEm) {
}
