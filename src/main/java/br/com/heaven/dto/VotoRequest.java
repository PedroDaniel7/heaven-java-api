package br.com.heaven.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Corpo de POST /api/validacoes. O validador vem só do token. */
public record VotoRequest(
        @Schema(example = "12") @NotNull(message = "obrigatório") Long idAcao,
        @Schema(example = "APROVAR") @NotBlank(message = "obrigatório") @Pattern(regexp = "APROVAR|REJEITAR", message = "use APROVAR ou REJEITAR") String voto,
        @Schema(example = "Evidência coerente") @Size(max = 500, message = "até 500 caracteres") String justificativa) {
}
