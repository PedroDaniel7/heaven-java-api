package br.com.heaven.bo;

import br.com.heaven.dao.NivelConfiancaDAO;
import br.com.heaven.dao.UsuarioDAO;
import br.com.heaven.model.NivelConfianca;
import br.com.heaven.model.Usuario;
import br.com.heaven.persistence.Jdbc;
import br.com.heaven.service.CodigoErro;
import br.com.heaven.service.RegraNegocioException;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Infraestrutura comum dos BOs: conexão, transação de escrita e verificações compartilhadas.
 * Escritas usam {@link #transacao}; leituras usam {@link #leitura}, sem mutex.
 */
abstract class BaseBO {

    @FunctionalInterface
    protected interface Operacao<T> {
        T executar(Connection c) throws SQLException;
    }

    private final DataSource dataSource;

    protected BaseBO(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource);
    }

    /**
     * Escrita: abre transação, bloqueia a linha MIN_VALIDACOES_POR_ACAO (mutex no banco, que
     * serializa os casos de uso e evita corridas com os triggers) e confirma ou desfaz.
     */
    protected <T> T transacao(Operacao<T> op) throws SQLException {
        try (Connection c = dataSource.getConnection()) {
            c.setAutoCommit(false);
            try {
                Jdbc.numero(c, "SELECT ID FROM PARAMETRO_SISTEMA WHERE NOME_PARAMETRO='MIN_VALIDACOES_POR_ACAO' FOR UPDATE");
                T resultado = op.executar(c);
                c.commit();
                return resultado;
            } catch (SQLException | RuntimeException e) {
                try {
                    c.rollback();
                } catch (SQLException rollback) {
                    e.addSuppressed(rollback);
                }
                throw e;
            }
        }
    }

    /** Leitura: sem mutex e sem gravar nada. */
    protected <T> T leitura(Operacao<T> op) throws SQLException {
        try (Connection c = dataSource.getConnection()) {
            return op.executar(c);
        }
    }

    protected Usuario ativo(Connection c, long id) throws SQLException {
        Usuario u = new UsuarioDAO(c).buscar(id)
                .orElseThrow(() -> new RegraNegocioException(CodigoErro.NAO_ENCONTRADO, "Usuário não encontrado."));
        if (!"ATIVO".equals(u.getStatusConta()))
            throw new RegraNegocioException(CodigoErro.PROIBIDO, "Conta não está ativa.");
        return u;
    }

    /**
     * Nível de confiança correspondente à reputação. Só grava a atualização do nível quando
     * {@code persistir} é verdadeiro (escritas); nas leituras apenas calcula.
     */
    protected NivelConfianca nivel(Connection c, Usuario u, boolean persistir) throws SQLException {
        List<NivelConfianca> niveis = new NivelConfiancaDAO(c).listar();
        NivelConfianca n = niveis.stream()
                .filter(x -> x.getReputacaoMinima().compareTo(u.getReputacaoAtual()) <= 0)
                .max(Comparator.comparing(NivelConfianca::getReputacaoMinima).thenComparing(NivelConfianca::getId))
                .orElseThrow(() -> new RegraNegocioException(CodigoErro.REGRA, "Cadastre um nível com reputação mínima zero."));
        if (u.getId() != null && !n.getId().equals(u.getIdNivelConfianca())) {
            if (persistir)
                Jdbc.executar(c, "UPDATE USUARIO SET ID_NIVEL_CONFIANCA=? WHERE ID=?", n.getId(), u.getId());
            u.setIdNivelConfianca(n.getId());
        }
        return n;
    }

    protected void bloqueio(Connection c, long id, String tipo) throws SQLException {
        if (Jdbc.numero(c, "SELECT COUNT(*) FROM PENALIZACAO WHERE ID_USUARIO=? AND STATUS='ATIVA' AND DATA_INICIO<=SYSDATE AND (DATA_FIM IS NULL OR DATA_FIM>SYSDATE) AND TIPO_PENALIZACAO IN (?, 'SUSPENSAO_CONTA')", id, tipo) > 0)
            throw new RegraNegocioException(CodigoErro.PROIBIDO, "Operação bloqueada por penalização ativa.");
    }
}
