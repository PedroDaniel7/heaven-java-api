package br.com.heaven.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Corpo de PUT /api/usuarios/me; {@code novaSenha} é opcional. */
public record AtualizarPerfilRequest(
        @NotBlank(message = "obrigatório") @Size(max = 120, message = "até 120 caracteres") String nome,
        @NotBlank(message = "obrigatório") @Size(max = 150, message = "até 150 caracteres") String email,
        @Size(min = 8, message = "mínimo de 8 caracteres") String novaSenha) {

    /** Nunca imprime a senha. */
    @Override
    public String toString() {
        return "AtualizarPerfilRequest[nome=" + nome + ", email=" + email + "]";
    }
}
