package br.com.heaven.resource;

import br.com.heaven.bo.PerfilUsuario;
import br.com.heaven.bo.UsuarioBO;
import br.com.heaven.config.Publico;
import br.com.heaven.dto.LoginRequest;
import br.com.heaven.dto.LoginResposta;
import br.com.heaven.dto.UsuarioDto;
import br.com.heaven.service.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.SQLException;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticação")
public class AuthResource {

    private final UsuarioBO usuarios;
    private final TokenService tokens;

    public AuthResource(UsuarioBO usuarios, TokenService tokens) {
        this.usuarios = usuarios;
        this.tokens = tokens;
    }

    @Publico
    @PostMapping("/login")
    @Operation(summary = "Login",
            description = "Devolve o token (12 h). Credencial inválida tem mensagem única; senha correta em conta inativa devolve 403.")
    @RespostasDeErro({400, 401, 403})
    public LoginResposta login(@Valid @RequestBody LoginRequest corpo) throws SQLException {
        PerfilUsuario perfil = usuarios.autenticar(corpo.email(), corpo.senha().toCharArray());
        TokenService.TokenEmitido emitido = tokens.emitir(perfil.usuario().getId());
        return new LoginResposta(emitido.token(), emitido.expiraEm(), UsuarioDto.de(perfil));
    }
}
