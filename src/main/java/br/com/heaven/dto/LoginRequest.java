package br.com.heaven.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/** Corpo de POST /api/auth/login. */
public record LoginRequest(
        @Schema(example = "ana@example.test") @NotBlank(message = "obrigatório") String email,
        @Schema(example = "umaSenhaBoa123") @NotBlank(message = "obrigatória") String senha) {

    /** Nunca imprime a senha. */
    @Override
    public String toString() {
        return "LoginRequest[email=" + email + "]";
    }
}
