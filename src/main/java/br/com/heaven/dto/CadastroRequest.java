package br.com.heaven.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Corpo de POST /api/usuarios. */
public record CadastroRequest(
        @Schema(example = "Ana Souza") @NotBlank(message = "obrigatório") @Size(max = 120, message = "até 120 caracteres") String nome,
        @Schema(example = "ana@example.test") @NotBlank(message = "obrigatório") @Size(max = 150, message = "até 150 caracteres") String email,
        @Schema(example = "umaSenhaBoa123") @NotBlank(message = "obrigatória") @Size(min = 8, message = "mínimo de 8 caracteres") String senha) {

    /** Nunca imprime a senha. */
    @Override
    public String toString() {
        return "CadastroRequest[nome=" + nome + ", email=" + email + "]";
    }
}
