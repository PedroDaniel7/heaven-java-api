package br.com.heaven.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;

/**
 * Pool Hikari pequeno de proposito: o schema da FIAP pode limitar sessoes.
 * Le SPRING_DATASOURCE_URL, SPRING_DATASOURCE_USERNAME e SPRING_DATASOURCE_PASSWORD.
 * O pool nao abre conexao na inicializacao, para a API subir mesmo com o banco fora do ar.
 */
@Configuration
public class DataSourceConfig {

    private static final String URL_AUSENTE = "jdbc:oracle:thin:@nao-configurado.invalid:1521:orcl";

    @Bean(destroyMethod = "close")
    public DataSource dataSource(Environment env) {
        HikariConfig cfg = new HikariConfig();
        cfg.setPoolName("heaven-pool");
        String url = env.getProperty("SPRING_DATASOURCE_URL", "");
        // Sem URL a API ainda sobe (para /api/saude); a consulta ao banco falha de forma controlada.
        cfg.setJdbcUrl(url.isBlank() ? URL_AUSENTE : url);
        cfg.setUsername(env.getProperty("SPRING_DATASOURCE_USERNAME", ""));
        cfg.setPassword(env.getProperty("SPRING_DATASOURCE_PASSWORD", ""));
        cfg.setMaximumPoolSize(4);
        cfg.setMinimumIdle(1);
        cfg.setConnectionTimeout(10_000);
        cfg.setInitializationFailTimeout(-1);
        return new HikariDataSource(cfg);
    }
}
