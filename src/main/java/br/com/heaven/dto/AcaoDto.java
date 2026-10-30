package br.com.heaven.dto;

import br.com.heaven.model.AcaoUsuario;
import br.com.heaven.model.TipoAcao;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Tipo {@code Acao} do contrato. A lista de votos nunca aparece. */
public record AcaoDto(long id, Tipo tipo, BigDecimal quantidade, String evidencia, String status,
                      long pontosPrevistos, LocalDateTime dataHora, LocalDateTime dataConclusao) {

    /** Resumo do tipo dentro da ação. */
    public record Tipo(long id, String nome, String unidade) {
    }

    public static AcaoDto de(AcaoUsuario a, TipoAcao t) {
        return new AcaoDto(a.getId(), new Tipo(t.getId(), t.getNome(), t.getUnidadeMedida()), a.getQuantidade(),
                a.getDescricaoEvidencia(), a.getStatus(), a.getPontosPrevistos(), a.getDataHora(), a.getDataConclusao());
    }
}
