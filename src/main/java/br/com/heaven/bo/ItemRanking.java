package br.com.heaven.bo;

/** Linha do ranking de usuários ativos. */
public record ItemRanking(long posicao, String nome, long pontos, String nivel, long streak) {
}
