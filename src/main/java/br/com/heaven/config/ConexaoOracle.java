package br.com.heaven.config;
import java.sql.*;
/** Preencha as constantes para a entrega acadêmica ou use as variáveis de ambiente. */
public final class ConexaoOracle {
    private static final String URL = "jdbc:oracle:thin:@//localhost:1521/FREEPDB1";
    private static final String USUARIO = "PREENCHER_USUARIO";
    private static final String SENHA = "PREENCHER_SENHA";
    private ConexaoOracle() {}
    public static Connection abrir() throws SQLException {
        String usuario = valor("HEAVEN_DB_USER", USUARIO);
        String senha = valor("HEAVEN_DB_PASSWORD", SENHA);
        if (usuario.startsWith("PREENCHER") || senha.startsWith("PREENCHER"))
            throw new SQLException("Configure URL, USUARIO e SENHA em ConexaoOracle ou HEAVEN_DB_URL/USER/PASSWORD.");
        return DriverManager.getConnection(valor("HEAVEN_DB_URL", URL), usuario, senha);
    }
    private static String valor(String nome, String padrao) {
        String v = System.getenv(nome); return v == null || v.isBlank() ? padrao : v;
    }
}
