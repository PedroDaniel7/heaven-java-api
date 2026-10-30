package br.com.heaven.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Corpo de POST /api/validacoes. O validador vem só do token. */
public record VotoRequest(
        @NotNull(message = "obrigatório") Long idAcao,
        @NotBlank(message = "obrigatório") @Pattern(regexp = "APROVAR|REJEITAR", message = "use APROVAR ou REJEITAR") String voto,
        @Size(max = 500, message = "até 500 caracteres") String justificativa) {
}
