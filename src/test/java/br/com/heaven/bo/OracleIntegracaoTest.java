package br.com.heaven.bo;

import br.com.heaven.dao.*;
import br.com.heaven.model.*;
import br.com.heaven.persistence.Jdbc;
import br.com.heaven.service.RegraNegocioException;
import br.com.heaven.service.Senhas;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.AbstractDataSource;

import javax.sql.DataSource;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.List;

/**
 * Integração real com Oracle, somente em schema de TESTES vazio de usuários (ex-TesteOracle).
 * Tudo termina em rollback. Exige SPRING_DATASOURCE_URL/USERNAME/PASSWORD. Fica ignorado para
 * não rodar no build: remova o @Disabled para executar manualmente.
 */
@Disabled("Exige um schema Oracle de testes, com o DDL e a carga inicial, sem usuários.")
class OracleIntegracaoTest {
    private static int testes;

    private static void ok(boolean b, String texto) {
        if (!b) throw new AssertionError(texto);
        testes++;
        System.out.println("OK " + texto);
    }

    @FunctionalInterface
    private interface Acao {
        void run() throws Exception;
    }

    private static void rejeita(Acao a, String texto) throws Exception {
        try {
            a.run();
            throw new AssertionError("Não rejeitou: " + texto);
        } catch (RegraNegocioException e) {
            ok(true, texto);
        }
    }

    // Cada caso de uso tem um savepoint: commit/close não encerram a transação externa de teste.
    private static Connection emprestar(Connection c) throws SQLException {
        Savepoint save = c.setSavepoint();
        return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class<?>[]{Connection.class}, (p, m, a) -> {
            switch (m.getName()) {
                case "close":
                case "commit":
                case "setAutoCommit":
                    return null;
                case "rollback":
                    c.rollback(save);
                    return null;
                default:
                    try {
                        return m.invoke(c, a);
                    } catch (InvocationTargetException e) {
                        throw e.getCause();
                    }
            }
        });
    }

    private static DataSource fonte(Connection c) {
        return new AbstractDataSource() {
            @Override
            public Connection getConnection() throws SQLException {
                return emprestar(c);
            }

            @Override
            public Connection getConnection(String usuario, String senha) throws SQLException {
                return emprestar(c);
            }
        };
    }

    private static <T> void crud(CrudDAO<T> dao, T item, java.util.function.Function<T, Long> id, java.util.function.Consumer<T> editar, java.util.function.Predicate<T> atualizado, String nome) throws Exception {
        dao.inserir(item);
        Long chave = id.apply(item);
        ok(chave != null, nome + " CREATE / identity");
        T lido = dao.buscar(chave).orElseThrow();
        ok(dao.listar().stream().anyMatch(x -> chave.equals(id.apply(x))), nome + " READ / listagem");
        editar.accept(lido);
        dao.atualizar(lido);
        ok(atualizado.test(dao.buscar(chave).orElseThrow()), nome + " UPDATE persistido");
        ok(dao.excluir(chave) && dao.buscar(chave).isEmpty(), nome + " DELETE");
        ok(!dao.excluir(chave), nome + " DELETE inexistente");
    }

    @Test
    void integracaoCompleta() throws Exception {
        try (Connection c = DriverManager.getConnection(System.getenv("SPRING_DATASOURCE_URL"), System.getenv("SPRING_DATASOURCE_USERNAME"), System.getenv("SPRING_DATASOURCE_PASSWORD"))) {
            c.setAutoCommit(false);
            try {
                ok(Jdbc.numero(c, "SELECT COUNT(*) FROM USUARIO") == 0, "schema sem usuários (pré-condição)");
                Jdbc.executar(c, "UPDATE PARAMETRO_SISTEMA SET VALOR=3 WHERE NOME_PARAMETRO='MIN_VALIDACOES_POR_ACAO'");
                Jdbc.executar(c, "UPDATE PARAMETRO_SISTEMA SET VALOR=60 WHERE NOME_PARAMETRO='PERCENTUAL_APROVACAO_MINIMO'");
                NivelConfianca n = new NivelConfianca();
                n.setNome("CRUD nivel");
                n.setReputacaoMinima(BigDecimal.ZERO);
                n.setPesoVoto(BigDecimal.ONE);
                n.setLimiteAcoesDia(5L);
                n.setLimiteValidacoesDia(10L);
                crud(new NivelConfiancaDAO(c), n, NivelConfianca::getId, x -> x.setNome("CRUD nivel editado"), x -> x.getNome().equals("CRUD nivel editado"), "NivelConfianca");
                ParametroSistema p = new ParametroSistema();
                p.setNomeParametro("CRUD_PARAM");
                p.setValor(BigDecimal.ONE);
                crud(new ParametroSistemaDAO(c), p, ParametroSistema::getId, x -> x.setValor(BigDecimal.TEN), x -> x.getValor().compareTo(BigDecimal.TEN) == 0, "ParametroSistema");
                Conquista co = new Conquista();
                co.setNome("CRUD conquista");
                co.setRequisito("Teste");
                crud(new ConquistaDAO(c), co, Conquista::getId, x -> x.setNome("CRUD conquista editada"), x -> x.getNome().equals("CRUD conquista editada"), "Conquista");
                TipoAcao t = new TipoAcao();
                t.setNome("CRUD tipo");
                t.setCategoria("OUTRO");
                t.setUnidadeMedida("un");
                t.setImpactoAmbientalEstimado(BigDecimal.ONE);
                t.setPontosBase(10L);
                t.setDificuldade("FACIL");
                crud(new TipoAcaoDAO(c), t, TipoAcao::getId, x -> x.setNome("CRUD tipo editado"), x -> x.getNome().equals("CRUD tipo editado"), "TipoAcao");
                long nivel = Jdbc.numero(c, "SELECT MIN(ID) FROM NIVEL_CONFIANCA");
                Usuario cru = new Usuario();
                cru.setNome("CRUD usuário");
                cru.setEmail("crud@heaven.test");
                cru.setSenhaHash(Senhas.criar("Teste12345".toCharArray()));
                cru.setIdNivelConfianca(nivel);
                crud(new UsuarioDAO(c), cru, Usuario::getId, x -> x.setNome("CRUD editado"), x -> x.getNome().equals("CRUD editado"), "Usuario");
                DataSource ds = fonte(c);
                UsuarioBO usuarios = new UsuarioBO(ds);
                AcaoBO acoes = new AcaoBO(ds);
                ValidacaoBO validacoes = new ValidacaoBO(ds);
                Usuario autor = usuarios.cadastrar("Autor", "autor@heaven.test", "Teste12345".toCharArray()).usuario();
                Usuario v1 = usuarios.cadastrar("Validador 1", "v1@heaven.test", "Teste12345".toCharArray()).usuario();
                Usuario v2 = usuarios.cadastrar("Validador 2", "v2@heaven.test", "Teste12345".toCharArray()).usuario();
                Usuario v3 = usuarios.cadastrar("Validador 3", "v3@heaven.test", "Teste12345".toCharArray()).usuario();
                ok(usuarios.autenticar("autor@heaven.test", "Teste12345".toCharArray()).usuario().getId().equals(autor.getId()), "login correto");
                rejeita(() -> usuarios.autenticar("autor@heaven.test", "SenhaErrada".toCharArray()), "login incorreto");
                IncidenteFraude inc = new IncidenteFraude();
                inc.setIdUsuario(autor.getId());
                inc.setTipoIncidente("COMPORTAMENTO_ANORMAL");
                inc.setNivelSeveridade("BAIXO");
                crud(new IncidenteFraudeDAO(c), inc, IncidenteFraude::getId, x -> x.setStatus("DESCARTADO"), x -> x.getStatus().equals("DESCARTADO"), "IncidenteFraude");
                long tipo = acoes.tiposAtivos().get(0).getId();
                AcaoUsuario a = acoes.registrarAcao(autor.getId(), tipo, BigDecimal.TEN, "Evidência de teste");
                ok("PENDENTE".equals(a.getStatus()), "registro pendente");
                ok(validacoes.sortearAcao(autor.getId()).isEmpty(), "sorteio exclui própria ação");
                rejeita(() -> validacoes.votar(autor.getId(), a.getId(), "APROVAR", "Teste"), "autovoto sem oferta");
                for (Usuario v : List.of(v1, v2, v3)) {
                    ok(validacoes.sortearAcao(v.getId()).orElseThrow().acao().getId().equals(a.getId()), "oferta elegível");
                    validacoes.votar(v.getId(), a.getId(), "APROVAR", "Evidência conferida");
                }
                ok("APROVADA".equals(new AcaoUsuarioDAO(c).buscar(a.getId()).orElseThrow().getStatus()), "trigger aprova no quórum");
                Usuario saldo = usuarios.perfil(autor.getId()).usuario();
                ok(saldo.getPontosAtuais().equals(a.getPontosPrevistos()), "pontos creditados uma vez");
                ok(saldo.getStreakAtual() == 1, "streak inicial por trigger");
                ok(usuarios.perfil(v1.getId()).usuario().getReputacaoAtual().compareTo(new BigDecimal("51")) == 0, "reputação por convergência");
                rejeita(() -> validacoes.votar(v1.getId(), a.getId(), "APROVAR", "Repetição"), "repetição sem oferta");
                AcaoUsuario b = acoes.registrarAcao(autor.getId(), tipo, BigDecimal.TEN, "Segunda evidência");
                for (Usuario v : List.of(v1, v2, v3)) {
                    validacoes.sortearAcao(v.getId());
                    validacoes.votar(v.getId(), b.getId(), "REJEITAR", "Evidência insuficiente");
                }
                ok("REJEITADA".equals(new AcaoUsuarioDAO(c).buscar(b.getId()).orElseThrow().getStatus()), "trigger rejeita no quórum");
                ok(usuarios.perfil(autor.getId()).usuario().getPontosAtuais().equals(a.getPontosPrevistos()), "rejeição sem pontos");
                AcaoUsuario cancelada = acoes.registrarAcao(autor.getId(), tipo, BigDecimal.ONE, "Cancelar teste");
                acoes.cancelarAcao(autor.getId(), cancelada.getId());
                ok("CANCELADA".equals(new AcaoUsuarioDAO(c).buscar(cancelada.getId()).orElseThrow().getStatus()), "cancelamento sem votos");
                rejeita(() -> acoes.cancelarAcao(autor.getId(), a.getId()), "não cancela ação concluída");
                long nivelAtual = usuarios.perfil(autor.getId()).usuario().getIdNivelConfianca();
                Jdbc.executar(c, "UPDATE NIVEL_CONFIANCA SET LIMITE_ACOES_DIA=3 WHERE ID=?", nivelAtual);
                rejeita(() -> acoes.registrarAcao(autor.getId(), tipo, BigDecimal.ONE, "Limite"), "limite de ações inclui canceladas");
                IncidenteFraude confirmado = new IncidenteFraude();
                confirmado.setIdUsuario(autor.getId());
                confirmado.setTipoIncidente("COMPORTAMENTO_ANORMAL");
                confirmado.setNivelSeveridade("MEDIO");
                confirmado.setStatus("CONFIRMADO");
                new IncidenteFraudeDAO(c).inserir(confirmado);
                validacoes.aplicarPenalizacao(confirmado.getId(), 999L, new BigDecimal("100"));
                ok(usuarios.perfil(autor.getId()).usuario().getPontosAtuais() == 0, "penalização respeita saldo");
                ok(usuarios.perfil(autor.getId()).usuario().getReputacaoAtual().signum() == 0, "penalização respeita piso reputação");
                rejeita(() -> validacoes.aplicarPenalizacao(confirmado.getId(), 1L, BigDecimal.ONE), "penalização idempotente por incidente");
                ok(!usuarios.ranking(20).isEmpty(), "view de ranking");
                System.out.println("TOTAL ORACLE: " + testes + " verificações. Dados serão desfeitos.");
            } finally {
                c.rollback();
                System.out.println("ROLLBACK concluído (identities podem deixar lacunas).");
            }
        }
    }
}
