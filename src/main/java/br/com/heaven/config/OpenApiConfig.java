package br.com.heaven.config;

import br.com.heaven.resource.ErroResposta;
import br.com.heaven.resource.RespostasDeErro;
import br.com.heaven.service.CodigoErro;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.Map;

/** Documentação OpenAPI (Swagger UI em /swagger-ui/index.html, JSON em /v3/api-docs). */
@Configuration
public class OpenApiConfig {

    public static final String BEARER = "bearerAuth";
    public static final String CHAVE_ADMIN = "adminKey";

    private static final Map<Integer, String> DESCRICAO_ERRO = Map.of(
            400, "Dados inválidos (VALIDACAO)",
            401, "Não autenticado (NAO_AUTENTICADO)",
            403, "Proibido (PROIBIDO)",
            404, "Não encontrado (NAO_ENCONTRADO)",
            409, "Conflito (CONFLITO ou OFERTA_EXPIRADA)",
            422, "Regra de negócio violada (REGRA)",
            503, "Banco indisponível (BANCO_INDISPONIVEL)");

    @Bean
    public OpenAPI heavenOpenApi() {
        return new OpenAPI()
                .info(new Info().title("API Heaven").version("1.0.0")
                        .description("Ações sustentáveis validadas pela comunidade. Todo erro tem o mesmo corpo "
                                + "(status, codigo, mensagem, campos, dataHora); decida por `codigo`, não pelo texto de `mensagem`. "
                                + "Rotas autenticadas usam `Authorization: Bearer <token>` (obtido em POST /api/auth/login, válido por 12 h); "
                                + "as de administração usam o header `X-Admin-Key`."))
                .components(new Components()
                        .addSecuritySchemes(BEARER, new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer"))
                        .addSecuritySchemes(CHAVE_ADMIN, new SecurityScheme().type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER).name(AuthInterceptor.CABECALHO_ADMIN)));
    }

    /** Registra o schema do corpo de erro nos componentes. */
    @Bean
    public OpenApiCustomizer esquemaDeErro() {
        return openApi -> ModelConverters.getInstance()
                .readAll(new AnnotatedType(ErroResposta.class))
                .forEach((nome, schema) -> openApi.getComponents().addSchemas(nome, schema));
    }

    /** Acrescenta a cada operação as respostas de erro declaradas em {@link RespostasDeErro}. */
    @Bean
    public OperationCustomizer respostasDeErro() {
        return (operation, handlerMethod) -> {
            RespostasDeErro erros = handlerMethod.getMethodAnnotation(RespostasDeErro.class);
            if (erros == null) return operation;
            if (operation.getResponses() == null) operation.setResponses(new ApiResponses());
            if (erros.sucesso() != 200) {
                ApiResponse ok = operation.getResponses().remove("200");
                if (ok == null) ok = new ApiResponse();
                if (erros.sucesso() == 204) ok.setContent(null);
                ok.setDescription(erros.sucesso() == 204 ? "Sem conteúdo" : "Criado");
                operation.getResponses().addApiResponse(String.valueOf(erros.sucesso()), ok);
            }
            for (int status : erros.value()) {
                CodigoErro codigo = CodigoErro.porStatus(status);
                Map<String, Object> exemplo = new LinkedHashMap<>();
                exemplo.put("status", status);
                exemplo.put("codigo", codigo.name());
                exemplo.put("mensagem", DESCRICAO_ERRO.getOrDefault(status, codigo.name()));
                exemplo.put("campos", status == 400 ? new Object[]{Map.of("campo", "email", "mensagem", "obrigatório")} : new Object[0]);
                exemplo.put("dataHora", "2026-10-30T00:18:20-03:00");
                MediaType tipo = new MediaType().schema(new Schema<>().$ref("#/components/schemas/ErroResposta")).example(exemplo);
                operation.getResponses().addApiResponse(String.valueOf(status), new ApiResponse()
                        .description(DESCRICAO_ERRO.getOrDefault(status, codigo.name()))
                        .content(new Content().addMediaType("application/json", tipo)));
            }
            return operation;
        };
    }
}
