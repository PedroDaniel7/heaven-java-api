package br.com.heaven.resource;

import br.com.heaven.bo.TipoAcaoAdminBO;
import br.com.heaven.config.AuthInterceptor;
import br.com.heaven.model.TipoAcao;
import br.com.heaven.service.ChaveAdmin;
import br.com.heaven.service.CodigoErro;
import br.com.heaven.service.RegraNegocioException;
import br.com.heaven.service.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Rotas /api/admin: só a chave X-Admin-Key vale (nada de Bearer), e o formato de erro do contrato. */
class AdminTipoAcaoResourceTest {

    private static final String CHAVE = "chave-admin-de-teste-com-24+";
    private static final String CORPO = "{\"nome\":\"Compostagem\",\"desafio\":\"Faça adubo\",\"categoria\":\"RECICLAGEM\","
            + "\"unidade\":\"kg\",\"impactoAmbientalEstimado\":0.5,\"pontosBase\":10,\"dificuldade\":\"MEDIA\",\"ativo\":false}";

    private final TipoAcaoAdminBO bo = mock(TipoAcaoAdminBO.class);
    private MockMvc mvc;

    private static MockMvc montar(TipoAcaoAdminBO bo, String chaveDoServidor) {
        TokenService tokens = new TokenService("segredo-de-teste-com-mais-de-32-caracteres");
        return MockMvcBuilders.standaloneSetup(new AdminTipoAcaoResource(bo))
                .setControllerAdvice(new ApiExceptionHandler())
                .addMappedInterceptors(new String[]{"/api/**"}, new AuthInterceptor(tokens, new ChaveAdmin(chaveDoServidor)))
                .build();
    }

    private static TipoAcao tipo() {
        TipoAcao t = new TipoAcao();
        t.setId(7L);
        t.setNome("Compostagem");
        t.setDescricao("Faça adubo");
        t.setCategoria("RECICLAGEM");
        t.setUnidadeMedida("kg");
        t.setImpactoAmbientalEstimado(new BigDecimal("0.50"));
        t.setPontosBase(10L);
        t.setDificuldade("MEDIA");
        t.setStatusAtivo("N");
        return t;
    }

    @BeforeEach
    void preparar() {
        mvc = montar(bo, CHAVE);
    }

    @Test
    void semChaveOuComChaveErradaVira401() throws Exception {
        mvc.perform(get("/api/admin/tipos-acao")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401)).andExpect(jsonPath("$.codigo").value("NAO_AUTENTICADO"));
        mvc.perform(get("/api/admin/tipos-acao").header("X-Admin-Key", CHAVE + "x")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/tipos-acao").header("X-Admin-Key", "")).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/admin/tipos-acao/7")).andExpect(status().isUnauthorized());
    }

    @Test
    void tokenDeUsuarioNaoSubstituiAChave() throws Exception {
        String bearer = "Bearer " + new TokenService("segredo-de-teste-com-mais-de-32-caracteres").emitir(1).token();
        mvc.perform(get("/api/admin/tipos-acao").header("Authorization", bearer)).andExpect(status().isUnauthorized());
    }

    @Test
    void chaveCorretaListaSemPrecisarDeToken() throws Exception {
        when(bo.listar()).thenReturn(List.of(tipo()));
        mvc.perform(get("/api/admin/tipos-acao").header("X-Admin-Key", CHAVE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(7))
                .andExpect(jsonPath("$[0].desafio").value("Faça adubo"))
                .andExpect(jsonPath("$[0].ativo").value(false))
                .andExpect(jsonPath("$[0].impactoAmbientalEstimado").value(0.5))
                .andExpect(jsonPath("$[0].pontosBase").value(10));
    }

    @Test
    void servidorSemChaveOuComChaveCurtaRecusaTudoMasContinuaDePe() throws Exception {
        for (String fraca : new String[]{"", "curta-demais", null}) {
            MockMvc m = montar(bo, fraca);
            m.perform(get("/api/admin/tipos-acao").header("X-Admin-Key", fraca == null ? "x" : fraca)).andExpect(status().isUnauthorized());
        }
    }

    @Test
    void criarDevolve201() throws Exception {
        when(bo.criar(any())).thenReturn(tipo());
        mvc.perform(post("/api/admin/tipos-acao").header("X-Admin-Key", CHAVE).contentType(MediaType.APPLICATION_JSON).content(CORPO))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.nome").value("Compostagem"));
    }

    @Test
    void validacaoEspelhaOsChecksDoBanco() throws Exception {
        String ruim = "{\"nome\":\"\",\"categoria\":\"INEXISTENTE\",\"unidade\":\"kg\",\"impactoAmbientalEstimado\":0,"
                + "\"pontosBase\":1000000,\"dificuldade\":\"IMPOSSIVEL\"}";
        mvc.perform(post("/api/admin/tipos-acao").header("X-Admin-Key", CHAVE).contentType(MediaType.APPLICATION_JSON).content(ruim))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"))
                .andExpect(jsonPath("$.campos[?(@.campo=='nome')]").exists())
                .andExpect(jsonPath("$.campos[?(@.campo=='categoria')]").exists())
                .andExpect(jsonPath("$.campos[?(@.campo=='impactoAmbientalEstimado')]").exists())
                .andExpect(jsonPath("$.campos[?(@.campo=='pontosBase')]").exists())
                .andExpect(jsonPath("$.campos[?(@.campo=='dificuldade')]").exists());
    }

    @Test
    void nomeRepetidoVira409SemVazarOra() throws Exception {
        when(bo.criar(any())).thenThrow(new SQLException("ORA-00001: unique constraint (HEAVEN.UK_TIPO_ACAO_NOME) violated", "23000", 1));
        mvc.perform(post("/api/admin/tipos-acao").header("X-Admin-Key", CHAVE).contentType(MediaType.APPLICATION_JSON).content(CORPO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLITO"))
                .andExpect(jsonPath("$.campos[0].campo").value("nome"))
                .andExpect(content().string(not(containsString("ORA-"))));
    }

    @Test
    void atualizarInexistenteVira404() throws Exception {
        when(bo.atualizar(anyLong(), any())).thenThrow(new RegraNegocioException(CodigoErro.NAO_ENCONTRADO, "Tipo não encontrado."));
        mvc.perform(put("/api/admin/tipos-acao/99").header("X-Admin-Key", CHAVE).contentType(MediaType.APPLICATION_JSON).content(CORPO))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.codigo").value("NAO_ENCONTRADO"));
    }

    @Test
    void apagarTipoEmUsoVira409DeUmOuOutroJeito() throws Exception {
        doThrow(new RegraNegocioException(CodigoErro.CONFLITO, "Tipo em uso por ações; desative-o em vez de apagar.")).when(bo).excluir(7);
        mvc.perform(delete("/api/admin/tipos-acao/7").header("X-Admin-Key", CHAVE))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("CONFLITO"));
        // Se a checagem do BO fosse contornada, a FK do banco (ORA-02292) também vira 409 sem vazar o código.
        doThrow(new SQLException("ORA-02292: integrity constraint (HEAVEN.FK_ACAO_USUARIO_TIPO) violated - child record found", "23000", 2292))
                .when(bo).excluir(8);
        mvc.perform(delete("/api/admin/tipos-acao/8").header("X-Admin-Key", CHAVE))
                .andExpect(status().isConflict()).andExpect(content().string(not(containsString("ORA-"))));
    }

    @Test
    void apagarDevolve204() throws Exception {
        mvc.perform(delete("/api/admin/tipos-acao/7").header("X-Admin-Key", CHAVE)).andExpect(status().isNoContent());
    }
}
