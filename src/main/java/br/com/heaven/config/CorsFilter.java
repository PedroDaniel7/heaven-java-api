package br.com.heaven.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Libera o front-end (CORS). Origens permitidas em HEAVEN_CORS_ORIGINS, separadas por virgula;
 * sem a variavel, apenas http://localhost:5173.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorsFilter extends OncePerRequestFilter {

    private static final String PADRAO = "http://localhost:5173";
    private final List<String> origensPermitidas;

    public CorsFilter(@Value("${HEAVEN_CORS_ORIGINS:}") String origens) {
        String lista = origens == null || origens.isBlank() ? PADRAO : origens;
        this.origensPermitidas = Arrays.stream(lista.split(","))
                .map(String::strip)
                .filter(o -> !o.isEmpty())
                .map(o -> o.endsWith("/") ? o.substring(0, o.length() - 1) : o)
                .toList();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String origem = req.getHeader("Origin");
        boolean permitida = origem != null && origensPermitidas.contains(origem);

        if (permitida) {
            res.setHeader("Access-Control-Allow-Origin", origem);
            res.addHeader("Vary", "Origin");
        }

        boolean preflight = "OPTIONS".equalsIgnoreCase(req.getMethod())
                && req.getHeader("Access-Control-Request-Method") != null;
        if (preflight) {
            if (permitida) {
                res.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
                res.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
                res.setHeader("Access-Control-Max-Age", "3600");
                res.setStatus(HttpServletResponse.SC_OK);
            } else {
                res.setStatus(HttpServletResponse.SC_FORBIDDEN);
            }
            return;
        }
        chain.doFilter(req, res);
    }
}
