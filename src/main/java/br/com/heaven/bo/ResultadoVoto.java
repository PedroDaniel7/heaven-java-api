package br.com.heaven.bo;

/** Resultado de um voto: a votação da ação foi encerrada por este voto ou ainda está aberta. */
public record ResultadoVoto(long idAcao, String voto, boolean acaoConcluida) {
}
