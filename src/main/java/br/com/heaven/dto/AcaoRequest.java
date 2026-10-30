package br.com.heaven.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Corpo de POST /api/acoes. O usuário vem só do token. */
public record AcaoRequest(
        @Schema(example = "1") @NotNull(message = "obrigatório") Long idTipoAcao,
        @NotNull(message = "obrigatória") @Positive(message = "deve ser maior que zero")
        @Schema(example = "2.5") @Digits(integer = 8, fraction = 2, message = "até 8 inteiros e 2 casas decimais") BigDecimal quantidade,
        @Schema(example = "Fui de metrô ao trabalho") @NotBlank(message = "obrigatória") @Size(max = 500, message = "até 500 caracteres") String evidencia) {
}
