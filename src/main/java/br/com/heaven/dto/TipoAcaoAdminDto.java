package br.com.heaven.dto;

import br.com.heaven.model.TipoAcao;

import java.math.BigDecimal;

/** Tipo de ação como a administração o vê, inclusive inativo e com os valores de cálculo. */
public record TipoAcaoAdminDto(long id, String nome, String desafio, String categoria, String unidade,
                               BigDecimal impactoAmbientalEstimado, long pontosBase, String dificuldade, boolean ativo) {

    public static TipoAcaoAdminDto de(TipoAcao t) {
        return new TipoAcaoAdminDto(t.getId(), t.getNome(), t.getDescricao(), t.getCategoria(), t.getUnidadeMedida(),
                t.getImpactoAmbientalEstimado(), t.getPontosBase(), t.getDificuldade(), "S".equals(t.getStatusAtivo()));
    }
}
