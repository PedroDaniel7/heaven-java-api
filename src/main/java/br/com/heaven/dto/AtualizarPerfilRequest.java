package br.com.heaven.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Corpo de PUT /api/usuarios/me; {@code novaSenha} é opcional. */
public record AtualizarPerfilRequest(
        @Schema(example = "Ana Souza") @NotBlank(message = "obrigatório") @Size(max = 120, message = "até 120 caracteres") String nome,
        @Schema(example = "ana@example.test") @NotBlank(message = "obrigatório") @Size(max = 150, message = "até 150 caracteres") String email,
        @Schema(example = "outraSenhaBoa456") @Size(min = 8, message = "mínimo de 8 caracteres") String novaSenha) {

    /** Nunca imprime a senha. */
    @Override
    public String toString() {
        return "AtualizarPerfilRequest[nome=" + nome + ", email=" + email + "]";
    }
}
