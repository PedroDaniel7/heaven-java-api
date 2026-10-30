package br.com.heaven.resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    @GetMapping
    public Map<String, Object> saude() {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("status", "OK");
        corpo.put("dataHora", OffsetDateTime.now().toString());
        return corpo;
    }

    @GetMapping("/banco")
    public ResponseEntity<Map<String, Object>> banco() {
        Map<String, Object> corpo = new LinkedHashMap<>();
        try {
            jdbc.queryForObject("SELECT 1 FROM DUAL", Integer.class);
            corpo.put("status", "OK");
            corpo.put("dataHora", OffsetDateTime.now().toString());
            return ResponseEntity.ok(corpo);
        } catch (Exception e) {
            // Apenas a classe da excecao vai ao log; a mensagem pode conter host/usuario.
            LOG.warn("Falha ao consultar o banco: {}", e.getClass().getSimpleName());
            corpo.put("status", "ERRO");
            corpo.put("codigo", "BANCO_INDISPONIVEL");
            corpo.put("mensagem", "Nao foi possivel conectar ao banco de dados.");
            corpo.put("dataHora", OffsetDateTime.now().toString());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(corpo);
        }
    }
}
