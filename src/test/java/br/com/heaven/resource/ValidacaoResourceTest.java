package br.com.heaven.resource;

import br.com.heaven.bo.AcaoBO;
import br.com.heaven.bo.ItemRanking;
import br.com.heaven.bo.ResultadoVoto;
import br.com.heaven.bo.Sorteio;
import br.com.heaven.bo.UsuarioBO;
import br.com.heaven.bo.ValidacaoBO;
import br.com.heaven.config.AuthInterceptor;
import br.com.heaven.model.AcaoUsuario;
import br.com.heaven.model.TipoAcao;
import br.com.heaven.service.CodigoErro;
import br.com.heaven.service.RegraNegocioException;
import br.com.heaven.service.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Formato das respostas de sorteio, voto e ranking e o mapeamento de erros, com os BOs simulados. */
class ValidacaoResourceTest {

    private final TokenService tokens = new TokenService("segredo-de-teste-com-mais-de-32-caracteres");
    private final ValidacaoBO validacoes = mock(ValidacaoBO.class);
    private final AcaoBO acoes = mock(AcaoBO.class);
    private final UsuarioBO usuarios = mock(UsuarioBO.class);
    private MockMvc mvc;
    private String auth;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.standaloneSetup(new ValidacaoResource(validacoes, acoes), new RankingResource(usuarios))
                .setControllerAdvice(new ApiExceptionHandler())
                .addMappedInterceptors(new String[]{"/api/**"}, new AuthInterceptor(tokens))
                .build();
        auth = "Bearer " + tokens.emitir(5).token();
    }

    @Test
    void sorteioDevolveAcaoParaValidarSemAutor() throws Exception {
        AcaoUsuario a = new AcaoUsuario();
        a.setId(9L);
        a.setIdUsuario(77L);
        a.setIdTipoAcao(1L);
        a.setQuantidade(new BigDecimal("2.5"));
        a.setDescricaoEvidencia("prova");
        TipoAcao t = new TipoAcao();
        t.setId(1L);
        t.setNome("Reciclagem");
        t.setDescricao("Separe o lixo");
        t.setUnidadeMedida("kg");
        when(validacoes.sortearAcao(5)).thenReturn(Optional.of(new Sorteio(a, Instant.now().plusSeconds(600))));
        when(acoes.tiposPorId()).thenReturn(Map.of(1L, t));
        mvc.perform(post("/api/validacoes/sorteio").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.acao.id").value(9))
                .andExpect(jsonPath("$.acao.tipo.desafio").value("Separe o lixo"))
                .andExpect(jsonPath("$.expiraEm").exists())
                .andExpect(content().string(not(containsString("77"))))
                .andExpect(content().string(not(containsString("idUsuario"))));
    }

    @Test
    void sorteioSemAcaoElegivelDevolve204() throws Exception {
        when(validacoes.sortearAcao(5)).thenReturn(Optional.empty());
        mvc.perform(post("/api/validacoes/sorteio").header("Authorization", auth)).andExpect(status().isNoContent());
    }

    @Test
    void sorteioSemTokenVira401() throws Exception {
        mvc.perform(post("/api/validacoes/sorteio")).andExpect(status().isUnauthorized());
    }

    @Test
    void votoValidoDevolve201ComResultado() throws Exception {
        when(validacoes.votar(5, 9, "APROVAR", null)).thenReturn(new ResultadoVoto(9, "APROVAR", true));
        mvc.perform(post("/api/validacoes").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idAcao\":9,\"voto\":\"APROVAR\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idAcao").value(9))
                .andExpect(jsonPath("$.voto").value("APROVAR"))
                .andExpect(jsonPath("$.acaoConcluida").value(true));
    }

    @Test
    void votoInvalidoOuSemCamposVira400() throws Exception {
        mvc.perform(post("/api/validacoes").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idAcao\":9,\"voto\":\"TALVEZ\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.campos[0].campo").value("voto"));
        mvc.perform(post("/api/validacoes").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void errosDoVotoMapeiamParaOsStatusDoContrato() throws Exception {
        Object[][] casos = {{CodigoErro.PROIBIDO, 403}, {CodigoErro.OFERTA_EXPIRADA, 409}, {CodigoErro.CONFLITO, 409},
                {CodigoErro.NAO_ENCONTRADO, 404}, {CodigoErro.REGRA, 422}};
        for (Object[] c : casos) {
            when(validacoes.votar(anyLong(), anyLong(), eq("REJEITAR"), isNull())).thenThrow(new RegraNegocioException((CodigoErro) c[0], "x"));
            mvc.perform(post("/api/validacoes").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"idAcao\":9,\"voto\":\"REJEITAR\"}"))
                    .andExpect(status().is((int) c[1])).andExpect(jsonPath("$.codigo").value(((CodigoErro) c[0]).name()));
        }
    }

    @Test
    void rankingEPublicoEUsaLimite20PorPadrao() throws Exception {
        when(usuarios.ranking(20)).thenReturn(List.of(new ItemRanking(1, "Ana", 100, "Colaborador", 2)));
        mvc.perform(get("/api/ranking"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].posicao").value(1))
                .andExpect(jsonPath("$[0].nome").value("Ana"))
                .andExpect(jsonPath("$[0].streak").value(2));
        verify(usuarios).ranking(20);
    }

    @Test
    void rankingComLimiteForaDaFaixaOuNaoNumericoVira400() throws Exception {
        when(usuarios.ranking(0)).thenThrow(new RegraNegocioException(CodigoErro.VALIDACAO, "O limite deve estar entre 1 e 100."));
        mvc.perform(get("/api/ranking").param("limite", "0")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/ranking").param("limite", "abc")).andExpect(status().isBadRequest());
    }
}
