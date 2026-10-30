package br.com.heaven.dto;

import br.com.heaven.model.TipoAcao;

/** Tipo {@code TipoAcao} do contrato; {@code desafio} vem de TIPO_ACAO.DESCRICAO. */
public record TipoAcaoDto(long id, String nome, String desafio, String categoria, String unidade, String dificuldade) {

    public static TipoAcaoDto de(TipoAcao t) {
        return new TipoAcaoDto(t.getId(), t.getNome(), t.getDescricao(), t.getCategoria(), t.getUnidadeMedida(), t.getDificuldade());
    }
}
