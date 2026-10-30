package br.com.heaven.resource;

import br.com.heaven.bo.UsuarioBO;
import br.com.heaven.config.AuthInterceptor;
import br.com.heaven.config.Publico;
import br.com.heaven.dto.AtualizarPerfilRequest;
import br.com.heaven.dto.CadastroRequest;
import br.com.heaven.dto.UsuarioDto;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.sql.SQLException;

/** Cadastro e perfil do usuário autenticado. */
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioResource {

    private final UsuarioBO usuarios;

    public UsuarioResource(UsuarioBO usuarios) {
        this.usuarios = usuarios;
    }

    @Publico
    @PostMapping
    public ResponseEntity<UsuarioDto> cadastrar(@Valid @RequestBody CadastroRequest corpo) throws SQLException {
        UsuarioDto criado = UsuarioDto.de(usuarios.cadastrar(corpo.nome(), corpo.email(), corpo.senha().toCharArray()));
        // Location relativo ao host, para não depender de cabeçalhos de proxy. O recurso do próprio usuário é /me.
        return ResponseEntity.created(URI.create("/api/usuarios/me")).body(criado);
    }

    @GetMapping("/me")
    public UsuarioDto perfil(@RequestAttribute(AuthInterceptor.USUARIO_ID) long usuarioId) throws SQLException {
        return UsuarioDto.de(usuarios.perfil(usuarioId));
    }

    @PutMapping("/me")
    public UsuarioDto atualizar(@RequestAttribute(AuthInterceptor.USUARIO_ID) long usuarioId,
                                @Valid @RequestBody AtualizarPerfilRequest corpo) throws SQLException {
        char[] novaSenha = corpo.novaSenha() == null ? null : corpo.novaSenha().toCharArray();
        return UsuarioDto.de(usuarios.atualizar(usuarioId, corpo.nome(), corpo.email(), novaSenha));
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> desativar(@RequestAttribute(AuthInterceptor.USUARIO_ID) long usuarioId) throws SQLException {
        usuarios.desativar(usuarioId);
        return ResponseEntity.noContent().build();
    }
}
