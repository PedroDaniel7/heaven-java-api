package br.com.heaven.resource;

import br.com.heaven.bo.UsuarioBO;
import br.com.heaven.config.AuthInterceptor;
import br.com.heaven.config.OpenApiConfig;
import br.com.heaven.config.Publico;
import br.com.heaven.dto.AtualizarPerfilRequest;
import br.com.heaven.dto.CadastroRequest;
import br.com.heaven.dto.UsuarioDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Usuários")
public class UsuarioResource {

    private final UsuarioBO usuarios;

    public UsuarioResource(UsuarioBO usuarios) {
        this.usuarios = usuarios;
    }

    @Publico
    @PostMapping
    @Operation(summary = "Cadastra um usuário",
            description = "Senha com no mínimo 8 caracteres. Responde 201 com o usuário e Location /api/usuarios/me.")
    @RespostasDeErro(value = {400, 409}, sucesso = 201)
    public ResponseEntity<UsuarioDto> cadastrar(@Valid @RequestBody CadastroRequest corpo) throws SQLException {
        UsuarioDto criado = UsuarioDto.de(usuarios.cadastrar(corpo.nome(), corpo.email(), corpo.senha().toCharArray()));
        // Location relativo ao host, para não depender de cabeçalhos de proxy. O recurso do próprio usuário é /me.
        return ResponseEntity.created(URI.create("/api/usuarios/me")).body(criado);
    }

    @GetMapping("/me")
    @Operation(summary = "Perfil do usuário autenticado")
    @SecurityRequirement(name = OpenApiConfig.BEARER)
    @RespostasDeErro({401})
    public UsuarioDto perfil(@RequestAttribute(AuthInterceptor.USUARIO_ID) long usuarioId) throws SQLException {
        return UsuarioDto.de(usuarios.perfil(usuarioId));
    }

    @PutMapping("/me")
    @Operation(summary = "Atualiza nome, e-mail e, opcionalmente, a senha")
    @SecurityRequirement(name = OpenApiConfig.BEARER)
    @RespostasDeErro({400, 401, 409})
    public UsuarioDto atualizar(@RequestAttribute(AuthInterceptor.USUARIO_ID) long usuarioId,
                                @Valid @RequestBody AtualizarPerfilRequest corpo) throws SQLException {
        char[] novaSenha = corpo.novaSenha() == null ? null : corpo.novaSenha().toCharArray();
        return UsuarioDto.de(usuarios.atualizar(usuarioId, corpo.nome(), corpo.email(), novaSenha));
    }

    @DeleteMapping("/me")
    @Operation(summary = "Desativa a conta",
            description = "Desativação lógica (INATIVO); o token deixa de valer em /usuarios/me.")
    @SecurityRequirement(name = OpenApiConfig.BEARER)
    @RespostasDeErro(value = {401}, sucesso = 204)
    public ResponseEntity<Void> desativar(@RequestAttribute(AuthInterceptor.USUARIO_ID) long usuarioId) throws SQLException {
        usuarios.desativar(usuarioId);
        return ResponseEntity.noContent().build();
    }
}
