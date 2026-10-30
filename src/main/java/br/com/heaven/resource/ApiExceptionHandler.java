package br.com.heaven.resource;

import br.com.heaven.service.CodigoErro;
import br.com.heaven.service.RegraNegocioException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.sql.SQLException;
import java.sql.SQLRecoverableException;
import java.sql.SQLTimeoutException;
import java.sql.SQLTransientConnectionException;
import java.sql.SQLNonTransientConnectionException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Traduz exceções no corpo de erro do contrato. Nunca devolve SQL, ORA-, host nem stack trace:
 * o detalhe técnico vai somente para o log do servidor.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);
    private static final int ORA_VIOLACAO_UNICA = 1;
    private static final Pattern RESTRICAO = Pattern.compile("\\(([^)]*)\\)");

    /** Restrições únicas conhecidas: mensagem amigável e campo. Qualquer outra recebe texto genérico. */
    private static final Map<String, ErroResposta.Campo> RESTRICOES_UNICAS = Map.of(
            "UK_USUARIO_EMAIL", new ErroResposta.Campo("email", "já cadastrado"),
            "UK_TIPO_ACAO_NOME", new ErroResposta.Campo("nome", "já cadastrado"),
            "UK_VALIDACAO_ACAO_VALIDADOR", new ErroResposta.Campo("idAcao", "voto já registrado"));
    private static final Map<String, String> MENSAGENS_UNICAS = Map.of(
            "UK_USUARIO_EMAIL", "E-mail já cadastrado.",
            "UK_TIPO_ACAO_NOME", "Nome já cadastrado.",
            "UK_VALIDACAO_ACAO_VALIDADOR", "Voto duplicado.");

    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<ErroResposta> regra(RegraNegocioException e) {
        return resposta(e.getCodigo(), e.getMessage(), List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResposta> validacao(MethodArgumentNotValidException e) {
        List<ErroResposta.Campo> campos = e.getBindingResult().getFieldErrors().stream()
                .map(f -> new ErroResposta.Campo(f.getField(), f.getDefaultMessage()))
                .toList();
        return resposta(CodigoErro.VALIDACAO, "Dados inválidos.", campos);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, TypeMismatchException.class,
            MissingServletRequestParameterException.class, ConstraintViolationException.class,
            HandlerMethodValidationException.class, HttpMediaTypeNotSupportedException.class})
    public ResponseEntity<ErroResposta> requisicaoInvalida(Exception e) {
        return resposta(CodigoErro.VALIDACAO, "Requisição inválida.", List.of());
    }

    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class, HttpRequestMethodNotSupportedException.class})
    public ResponseEntity<ErroResposta> naoEncontrado(Exception e) {
        return resposta(CodigoErro.NAO_ENCONTRADO, "Recurso não encontrado.", List.of());
    }

    @ExceptionHandler({SQLException.class, DataAccessException.class})
    public ResponseEntity<ErroResposta> banco(Exception e) {
        SQLException sql = primeiraSqlException(e);
        if (sql != null && sql.getErrorCode() == ORA_VIOLACAO_UNICA) return conflitoUnico(sql);
        if (indisponivel(e)) {
            LOG.warn("Banco indisponível: {}", e.getClass().getSimpleName());
            return resposta(CodigoErro.BANCO_INDISPONIVEL, "Não foi possível conectar ao banco de dados.", List.of());
        }
        return erroInterno(e);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResposta> inesperado(Exception e) {
        return erroInterno(e);
    }

    private ResponseEntity<ErroResposta> conflitoUnico(SQLException sql) {
        String restricao = nomeRestricao(sql.getMessage());
        ErroResposta.Campo campo = RESTRICOES_UNICAS.get(restricao);
        String mensagem = MENSAGENS_UNICAS.getOrDefault(restricao, "Registro já existente.");
        return resposta(CodigoErro.CONFLITO, mensagem, campo == null ? List.of() : List.of(campo));
    }

    private ResponseEntity<ErroResposta> erroInterno(Exception e) {
        LOG.error("Erro interno não tratado", e);
        return resposta(CodigoErro.ERRO_INTERNO, "Erro interno. Tente novamente mais tarde.", List.of());
    }

    private static ResponseEntity<ErroResposta> resposta(CodigoErro codigo, String mensagem, List<ErroResposta.Campo> campos) {
        ErroResposta corpo = new ErroResposta(codigo.getStatus(), codigo.name(), mensagem, campos, OffsetDateTime.now().toString());
        return ResponseEntity.status(HttpStatus.valueOf(codigo.getStatus())).body(corpo);
    }

    /** Nome da restrição em "ORA-00001: unique constraint (SCHEMA.NOME) violated"; só é comparado a uma lista fixa. */
    private static String nomeRestricao(String mensagem) {
        if (mensagem == null) return "";
        Matcher m = RESTRICAO.matcher(mensagem);
        if (!m.find()) return "";
        String completo = m.group(1);
        return completo.substring(completo.lastIndexOf('.') + 1).toUpperCase();
    }

    private static SQLException primeiraSqlException(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof SQLException s) return s;
            if (t.getCause() == t) break;
        }
        return null;
    }

    private static boolean indisponivel(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof SQLTransientConnectionException || t instanceof SQLNonTransientConnectionException
                    || t instanceof SQLRecoverableException || t instanceof SQLTimeoutException
                    || t.getClass().getSimpleName().equals("CannotGetJdbcConnectionException")) return true;
            if (t.getCause() == t) break;
        }
        return false;
    }
}
