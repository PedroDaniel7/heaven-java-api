package br.com.heaven.dto;

import jakarta.validation.constraints.NotBlank;

/** Corpo de POST /api/auth/login. */
public record LoginRequest(
        @NotBlank(message = "obrigatório") String email,
        @NotBlank(message = "obrigatória") String senha) {

    /** Nunca imprime a senha. */
    @Override
    public String toString() {
        return "LoginRequest[email=" + email + "]";
    }
}
