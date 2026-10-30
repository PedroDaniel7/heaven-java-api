package br.com.heaven.config;

import br.com.heaven.service.CodigoErro;
import br.com.heaven.service.RegraNegocioException;
import br.com.heaven.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Exige token em toda rota de /api que não esteja marcada com {@link Publico} (negado por padrão).
 * O id do usuário vai para o atributo {@link #USUARIO_ID} da requisição, lido com {@code @RequestAttribute}.
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    public static final String USUARIO_ID = "heaven.usuarioId";
    private static final String PREFIXO = "Bearer ";

    private final TokenService tokens;

    public AuthInterceptor(TokenService tokens) {
        this.tokens = tokens;
    }

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) {
        if (handler instanceof HandlerMethod m && m.hasMethodAnnotation(Publico.class)) return true;
        // O erro sai pelo ApiExceptionHandler, com o mesmo corpo de qualquer outro.
        String cabecalho = req.getHeader("Authorization");
        if (cabecalho == null || !cabecalho.regionMatches(true, 0, PREFIXO, 0, PREFIXO.length()))
            throw new RegraNegocioException(CodigoErro.NAO_AUTENTICADO, "Token ausente, inválido ou expirado.");
        req.setAttribute(USUARIO_ID, tokens.validar(cabecalho.substring(PREFIXO.length()).strip()));
        return true;
    }
}
