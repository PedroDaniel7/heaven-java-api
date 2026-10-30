package br.com.heaven.dto;

import br.com.heaven.model.AcaoUsuario;
import br.com.heaven.model.TipoAcao;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

/** Resposta de POST /api/validacoes/sorteio: {@code AcaoParaValidar} sem autor (anonimato) e o fim da oferta. */
public record SorteioResposta(AcaoParaValidar acao, OffsetDateTime expiraEm) {

    /** Tipo {@code AcaoParaValidar} do contrato. */
    public record AcaoParaValidar(long id, Tipo tipo, BigDecimal quantidade, String evidencia, LocalDateTime dataHora) {
    }

    /** Tipo resumido, com o desafio (TIPO_ACAO.DESCRICAO). */
    public record Tipo(String nome, String unidade, String desafio) {
    }

    public static SorteioResposta de(AcaoUsuario a, TipoAcao t, OffsetDateTime expiraEm) {
        return new SorteioResposta(new AcaoParaValidar(a.getId(), new Tipo(t.getNome(), t.getUnidadeMedida(), t.getDescricao()),
                a.getQuantidade(), a.getDescricaoEvidencia(), a.getDataHora()), expiraEm);
    }
}
