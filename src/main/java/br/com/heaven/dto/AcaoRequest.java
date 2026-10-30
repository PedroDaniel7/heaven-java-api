package br.com.heaven.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Corpo de POST /api/acoes. O usuário vem só do token. */
public record AcaoRequest(
        @NotNull(message = "obrigatório") Long idTipoAcao,
        @NotNull(message = "obrigatória") @Positive(message = "deve ser maior que zero")
        @Digits(integer = 8, fraction = 2, message = "até 8 inteiros e 2 casas decimais") BigDecimal quantidade,
        @NotBlank(message = "obrigatória") @Size(max = 500, message = "até 500 caracteres") String evidencia) {
}
