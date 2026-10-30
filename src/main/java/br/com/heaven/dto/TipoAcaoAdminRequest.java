package br.com.heaven.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Corpo de POST e PUT /api/admin/tipos-acao; espelha as colunas e os CHECKs de TIPO_ACAO. */
public record TipoAcaoAdminRequest(
        @Schema(description = "Nome único (TIPO_ACAO.NOME, até 100)", example = "Compostagem doméstica")
        @NotBlank(message = "obrigatório") @Size(max = 100, message = "até 100 caracteres") String nome,
        @Schema(description = "Desafio mostrado ao validador (TIPO_ACAO.DESCRICAO, opcional, até 500)", example = "Separe os resíduos orgânicos e produza adubo")
        @Size(max = 500, message = "até 500 caracteres") String desafio,
        @Schema(description = "Categoria", allowableValues = {"TRANSPORTE", "ENERGIA", "AGUA", "RECICLAGEM", "PLANTIO", "CONSUMO_CONSCIENTE", "OUTRO"}, example = "RECICLAGEM")
        @NotBlank(message = "obrigatória")
        @Pattern(regexp = "TRANSPORTE|ENERGIA|AGUA|RECICLAGEM|PLANTIO|CONSUMO_CONSCIENTE|OUTRO", message = "categoria inválida") String categoria,
        @Schema(description = "Unidade de medida (até 20)", example = "kg")
        @NotBlank(message = "obrigatória") @Size(max = 20, message = "até 20 caracteres") String unidade,
        @Schema(description = "Impacto ambiental estimado por unidade (maior que 0, até 8 inteiros e 2 casas)", example = "0.50")
        @NotNull(message = "obrigatório") @Positive(message = "deve ser maior que zero")
        @Digits(integer = 8, fraction = 2, message = "até 8 inteiros e 2 casas decimais") BigDecimal impactoAmbientalEstimado,
        @Schema(description = "Pontos base (1 a 999999)", example = "10")
        @NotNull(message = "obrigatório") @Positive(message = "deve ser maior que zero")
        @Max(value = 999999, message = "no máximo 999999") Long pontosBase,
        @Schema(description = "Dificuldade", allowableValues = {"FACIL", "MEDIA", "DIFICIL"}, example = "MEDIA")
        @NotBlank(message = "obrigatória") @Pattern(regexp = "FACIL|MEDIA|DIFICIL", message = "dificuldade inválida") String dificuldade,
        @Schema(description = "Ativo para registro de ações. Ausente: true no POST e o valor atual no PUT", example = "true")
        Boolean ativo) {
}
