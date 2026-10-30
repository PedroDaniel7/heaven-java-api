package br.com.heaven.resource;

import br.com.heaven.config.Publico;
import br.com.heaven.service.CodigoErro;
import br.com.heaven.service.RegraNegocioException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/** Verificacao de saude da API e do banco. Nunca expõe SQL, host nem stack trace. */
@RestController
@RequestMapping("/api/saude")
public class SaudeResource {

    private static final Logger LOG = LoggerFactory.getLogger(SaudeResource.class);
    private final JdbcTemplate jdbc;

    public SaudeResource(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Publico
    @GetMapping
    public Map<String, Object> saude() {
        return corpoOk();
    }

    @Publico
    @GetMapping("/banco")
    public Map<String, Object> banco() {
        try {
            jdbc.queryForObject("SELECT 1 FROM DUAL", Integer.class);
        } catch (Exception e) {
            // Apenas a classe da excecao vai ao log; a mensagem pode conter host/usuario.
            LOG.warn("Falha ao consultar o banco: {}", e.getClass().getSimpleName());
            // O corpo 503 sai pelo ApiExceptionHandler, igual ao de qualquer outro erro.
            throw new RegraNegocioException(CodigoErro.BANCO_INDISPONIVEL, "Não foi possível conectar ao banco de dados.");
        }
        return corpoOk();
    }

    private static Map<String, Object> corpoOk() {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("status", "OK");
        corpo.put("dataHora", OffsetDateTime.now().toString());
        return corpo;
    }
}
