package br.com.heaven.resource;

import br.com.heaven.service.CodigoErro;
import br.com.heaven.service.RegraNegocioException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.sql.SQLException;
import java.sql.SQLTransientConnectionException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Verifica o formato de erro do contrato para cada código, sem Spring Boot completo e sem banco. */
class ApiExceptionHandlerTest {

    record Entrada(@NotBlank String nome) {
    }

    @RestController
    static class ControladorFalso {
        @GetMapping("/regra/{codigo}")
        void regra(@PathVariable CodigoErro codigo) {
            throw new RegraNegocioException(codigo, "mensagem de teste");
        }

        @GetMapping("/unica-email")
        void unicaEmail() throws SQLException {
            throw new SQLException("ORA-00001: unique constraint (HEAVEN.UK_USUARIO_EMAIL) violated", "23000", 1);
        }

        @GetMapping("/unica-outra")
        void unicaOutra() throws SQLException {
            throw new SQLException("ORA-00001: unique constraint (HEAVEN.UK_DESCONHECIDA) violated", "23000", 1);
        }

        @GetMapping("/sql")
        void sql() throws SQLException {
            throw new SQLException("ORA-00942: table or view does not exist: SELECT * FROM SEGREDO host=host-ficticio.exemplo", "42000", 942);
        }

        @GetMapping("/conexao")
        void conexao() throws SQLException {
            throw new SQLTransientConnectionException("heaven-pool - Connection is not available, request timed out");
        }

        @GetMapping("/inesperado")
        void inesperado() {
            throw new IllegalStateException("ORA-12345 SELECT secreto FROM tabela host=host-ficticio.exemplo");
        }

        @PostMapping("/validar")
        void validar(@Valid @RequestBody Entrada entrada) {
        }
    }

    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.standaloneSetup(new ControladorFalso()).setControllerAdvice(new ApiExceptionHandler()).build();
    }

    @ParameterizedTest
    @EnumSource(CodigoErro.class)
    void cadaCodigoDoContratoTemStatusECorpoCorretos(CodigoErro codigo) throws Exception {
        mvc.perform(get("/regra/" + codigo.name()))
                .andExpect(status().is(codigo.getStatus()))
                .andExpect(jsonPath("$.status").value(codigo.getStatus()))
                .andExpect(jsonPath("$.codigo").value(codigo.name()))
                .andExpect(jsonPath("$.mensagem").value("mensagem de teste"))
                .andExpect(jsonPath("$.campos").isArray())
                .andExpect(jsonPath("$.campos").isEmpty())
                .andExpect(jsonPath("$.dataHora").isNotEmpty());
    }

    @Test
    void statusDeCadaCodigoSegueOContrato() {
        org.junit.jupiter.api.Assertions.assertEquals(400, CodigoErro.VALIDACAO.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals(401, CodigoErro.NAO_AUTENTICADO.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals(403, CodigoErro.PROIBIDO.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals(404, CodigoErro.NAO_ENCONTRADO.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals(409, CodigoErro.CONFLITO.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals(422, CodigoErro.REGRA.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals(409, CodigoErro.OFERTA_EXPIRADA.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals(503, CodigoErro.BANCO_INDISPONIVEL.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals(500, CodigoErro.ERRO_INTERNO.getStatus());
    }

    @Test
    void erroDeValidacaoDeCorpoVira400ComCampos() throws Exception {
        mvc.perform(post("/validar").contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"))
                .andExpect(jsonPath("$.campos[0].campo").value("nome"))
                .andExpect(jsonPath("$.campos[0].mensagem").isNotEmpty());
    }

    @Test
    void jsonMalformadoVira400Validacao() throws Exception {
        mvc.perform(post("/validar").contentType(MediaType.APPLICATION_JSON).content("{nome:"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"));
    }

    @Test
    void ora00001DeEmailVira409ConflitoComCampo() throws Exception {
        mvc.perform(get("/unica-email"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.codigo").value("CONFLITO"))
                .andExpect(jsonPath("$.mensagem").value("E-mail já cadastrado."))
                .andExpect(jsonPath("$.campos[0].campo").value("email"));
    }

    @Test
    void ora00001DeRestricaoDesconhecidaVira409Generico() throws Exception {
        MvcResult r = mvc.perform(get("/unica-outra"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLITO"))
                .andExpect(jsonPath("$.mensagem").value("Registro já existente."))
                .andReturn();
        assertFalse(r.getResponse().getContentAsString().contains("UK_DESCONHECIDA"));
    }

    @Test
    void falhaDeConexaoVira503BancoIndisponivel() throws Exception {
        mvc.perform(get("/conexao"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.codigo").value("BANCO_INDISPONIVEL"));
    }

    @Test
    void outroErroDeSqlVira500SemDetalhe() throws Exception {
        MvcResult r = mvc.perform(get("/sql"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.codigo").value("ERRO_INTERNO"))
                .andReturn();
        semDetalheTecnico(r.getResponse().getContentAsString());
    }

    @Test
    void excecaoInesperadaVira500GenericoSemDetalhe() throws Exception {
        MvcResult r = mvc.perform(get("/inesperado"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.codigo").value("ERRO_INTERNO"))
                .andExpect(jsonPath("$.mensagem").value("Erro interno. Tente novamente mais tarde."))
                .andExpect(jsonPath("$.campos").isEmpty())
                .andReturn();
        semDetalheTecnico(r.getResponse().getContentAsString());
    }

    @Test
    void rotaInexistenteVira404NaoEncontrado() throws Exception {
        mvc.perform(get("/nao-existe"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NAO_ENCONTRADO"));
    }

    private static void semDetalheTecnico(String corpo) {
        for (String proibido : new String[]{"ORA-", "SELECT", "Exception", "host-ficticio", "secreto", "SEGREDO"})
            assertFalse(corpo.contains(proibido), "O corpo não pode conter: " + proibido);
    }
}
