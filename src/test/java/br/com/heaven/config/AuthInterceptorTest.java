package br.com.heaven.config;

import br.com.heaven.resource.ApiExceptionHandler;
import br.com.heaven.resource.SaudeResource;
import br.com.heaven.service.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RestController;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Interceptor de autenticação e erro do /saude/banco, sem banco: a rota protegida é um controller de teste. */
class AuthInterceptorTest {

    @RestController
    static class Fake {
        @GetMapping("/api/protegida")
        String protegida(@RequestAttribute(AuthInterceptor.USUARIO_ID) long id) {
            return "usuario-" + id;
        }

        @Publico
        @GetMapping("/api/aberta")
        String aberta() {
            return "ok";
        }
    }

    private final TokenService tokens = new TokenService("segredo-de-teste-com-mais-de-32-caracteres");
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.standaloneSetup(new Fake(), new SaudeResource(jdbc))
                .setControllerAdvice(new ApiExceptionHandler())
                .addMappedInterceptors(new String[]{"/api/**"}, new AuthInterceptor(tokens))
                .build();
    }

    @Test
    void rotaPublicaDispensaToken() throws Exception {
        mvc.perform(get("/api/aberta")).andExpect(status().isOk());
        mvc.perform(get("/api/saude")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("OK"));
    }

    @Test
    void rotaProtegidaSemTokenVira401NoFormatoDoContrato() throws Exception {
        mvc.perform(get("/api/protegida"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.codigo").value("NAO_AUTENTICADO"));
    }

    @Test
    void tokenValidoColocaOUsuarioNoContexto() throws Exception {
        String token = tokens.emitir(42).token();
        mvc.perform(get("/api/protegida").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().string("usuario-42"));
    }

    @Test
    void tokenAdulteradoOuEsquemaErradoVira401() throws Exception {
        String token = tokens.emitir(42).token();
        String adulterado = token.substring(0, token.length() - 1) + (token.endsWith("A") ? "B" : "A");
        mvc.perform(get("/api/protegida").header("Authorization", "Bearer " + adulterado)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/protegida").header("Authorization", "Basic " + token)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/protegida").header("Authorization", token)).andExpect(status().isUnauthorized());
    }

    @Test
    void saudeBancoForaDoArVira503ComCorpoDoContrato() throws Exception {
        when(jdbc.queryForObject(anyString(), any(Class.class)))
                .thenThrow(new CannotGetJdbcConnectionException("falha com host.secreto:1521", new java.sql.SQLException("ORA-12170")));
        mvc.perform(get("/api/saude/banco"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.codigo").value("BANCO_INDISPONIVEL"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("ORA-"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("secreto"))));
    }
}
