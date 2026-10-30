package br.com.heaven.dto;

import br.com.heaven.bo.PerfilUsuario;
import br.com.heaven.model.Usuario;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Tipo {@code Usuario} do contrato. Não carrega senhaHash, peso do voto nem nível interno. */
public record UsuarioDto(long id, String nome, String email, long pontos, BigDecimal reputacao, String nivel,
                         long streak, long maiorStreak, LocalDateTime dataCadastro) {

    public static UsuarioDto de(PerfilUsuario p) {
        Usuario u = p.usuario();
        return new UsuarioDto(u.getId(), u.getNome(), u.getEmail(), u.getPontosAtuais(), u.getReputacaoAtual(),
                p.nivel(), u.getStreakAtual(), u.getMaiorStreak(), u.getDataCadastro());
    }
}
