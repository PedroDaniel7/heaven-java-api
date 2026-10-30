package br.com.heaven.bo;

import br.com.heaven.dao.TipoAcaoDAO;
import br.com.heaven.model.TipoAcao;
import br.com.heaven.persistence.Jdbc;
import br.com.heaven.service.CodigoErro;
import br.com.heaven.service.RegraNegocioException;
import br.com.heaven.service.Regras;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import javax.sql.DataSource;

/** CRUD administrativo de tipos de ação. Nome repetido vira 409 pela restrição única do banco. */
@Service
public class TipoAcaoAdminBO extends BaseBO {

    private static final Set<String> CATEGORIAS = Set.of("TRANSPORTE", "ENERGIA", "AGUA", "RECICLAGEM", "PLANTIO", "CONSUMO_CONSCIENTE", "OUTRO");
    private static final Set<String> DIFICULDADES = Set.of("FACIL", "MEDIA", "DIFICIL");

    /** Dados de entrada; {@code ativo} nulo significa "ativo" na criação e "manter" na atualização. */
    public record Dados(String nome, String descricao, String categoria, String unidade,
                        BigDecimal impacto, Long pontosBase, String dificuldade, Boolean ativo) {
    }

    public TipoAcaoAdminBO(DataSource dataSource) {
        super(dataSource);
    }

    public List<TipoAcao> listar() throws SQLException {
        return leitura(c -> new TipoAcaoDAO(c).listar().stream().sorted(Comparator.comparing(TipoAcao::getId)).toList());
    }

    public TipoAcao criar(Dados d) throws SQLException {
        TipoAcao t = montar(new TipoAcao(), d);
        if (d.ativo() == null) t.setStatusAtivo("S");
        return transacao(c -> {
            new TipoAcaoDAO(c).inserir(t);
            return new TipoAcaoDAO(c).buscar(t.getId()).orElseThrow();
        });
    }

    public TipoAcao atualizar(long id, Dados d) throws SQLException {
        return transacao(c -> {
            TipoAcaoDAO dao = new TipoAcaoDAO(c);
            TipoAcao t = dao.buscar(id).orElseThrow(TipoAcaoAdminBO::naoEncontrado);
            montar(t, d);
            dao.atualizar(t);
            return dao.buscar(id).orElseThrow();
        });
    }

    /** Apaga de verdade; tipo com ações vinculadas é 409 (a alternativa é desativá-lo). */
    public void excluir(long id) throws SQLException {
        transacao(c -> {
            TipoAcaoDAO dao = new TipoAcaoDAO(c);
            dao.buscar(id).orElseThrow(TipoAcaoAdminBO::naoEncontrado);
            if (Jdbc.numero(c, "SELECT COUNT(*) FROM ACAO_USUARIO WHERE ID_TIPO_ACAO=?", id) > 0)
                throw new RegraNegocioException(CodigoErro.CONFLITO, "Tipo em uso por ações; desative-o em vez de apagar.");
            dao.excluir(id);
            return null;
        });
    }

    private static RegraNegocioException naoEncontrado() {
        return new RegraNegocioException(CodigoErro.NAO_ENCONTRADO, "Tipo não encontrado.");
    }

    /** Valida (espelhando os CHECKs de TIPO_ACAO) e copia os dados para a entidade. */
    private static TipoAcao montar(TipoAcao t, Dados d) {
        t.setNome(Regras.texto(d.nome(), "Nome", 100));
        t.setDescricao(Regras.textoOpcional(d.descricao(), "Desafio", 500));
        if (d.categoria() == null || !CATEGORIAS.contains(d.categoria()))
            throw new RegraNegocioException(CodigoErro.VALIDACAO, "Categoria inválida.");
        t.setCategoria(d.categoria());
        t.setUnidadeMedida(Regras.texto(d.unidade(), "Unidade", 20));
        if (d.impacto() == null || d.impacto().signum() <= 0 || d.impacto().stripTrailingZeros().scale() > 2
                || d.impacto().compareTo(new BigDecimal("99999999.99")) > 0)
            throw new RegraNegocioException(CodigoErro.VALIDACAO, "Impacto deve ser maior que zero, com até 8 inteiros e 2 casas.");
        t.setImpactoAmbientalEstimado(d.impacto());
        if (d.pontosBase() == null || d.pontosBase() < 1 || d.pontosBase() > 999999)
            throw new RegraNegocioException(CodigoErro.VALIDACAO, "Pontos base devem ficar entre 1 e 999999.");
        t.setPontosBase(d.pontosBase());
        if (d.dificuldade() == null || !DIFICULDADES.contains(d.dificuldade()))
            throw new RegraNegocioException(CodigoErro.VALIDACAO, "Dificuldade inválida.");
        t.setDificuldade(d.dificuldade());
        if (d.ativo() != null) t.setStatusAtivo(d.ativo() ? "S" : "N");
        return t;
    }
}
