package br.com.heaven.bo;

import br.com.heaven.model.Usuario;

/** Usuário com o nome do nível de confiança calculado a partir da reputação. */
public record PerfilUsuario(Usuario usuario, String nivel) {
}
