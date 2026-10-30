package br.com.heaven.bo;

import br.com.heaven.dao.AcaoUsuarioDAO;
import br.com.heaven.dao.TipoAcaoDAO;
import br.com.heaven.model.AcaoUsuario;
import br.com.heaven.model.NivelConfianca;
import br.com.heaven.model.TipoAcao;
import br.com.heaven.model.Usuario;
import br.com.heaven.persistence.Jdbc;
import br.com.heaven.service.CodigoErro;
import br.com.heaven.service.RegraNegocioException;
import br.com.heaven.service.Regras;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Tipos de ação e ações sustentáveis do usuário. */
@Service
public class AcaoBO extends BaseBO {

    public AcaoBO(DataSource dataSource) {
        super(dataSource);
    }

    public List<TipoAcao> tiposAtivos() throws SQLException {
        return leitura(c -> new TipoAcaoDAO(c).listar().stream().filter(t -> "S".equals(t.getStatusAtivo())).toList());
    }

    public AcaoUsuario registrarAcao(long usuario, long tipo, BigDecimal quantidade, String evidencia) throws SQLException {
        String ev = Regras.texto(evidencia, "Evidência", 500);
        return transacao(c -> {
            Usuario u = ativo(c, usuario);
            bloqueio(c, usuario, "BLOQUEIO_ACAO");
            NivelConfianca n = nivel(c, u, true);
            Regras.verificarLimite(Jdbc.numero(c, "SELECT COUNT(*) FROM ACAO_USUARIO WHERE ID_USUARIO=? AND DATA_HORA>=TRUNC(SYSDATE) AND DATA_HORA<TRUNC(SYSDATE)+1", usuario), n.getLimiteAcoesDia());
            TipoAcao t = new TipoAcaoDAO(c).buscar(tipo)
                    .orElseThrow(() -> new RegraNegocioException(CodigoErro.NAO_ENCONTRADO, "Tipo não encontrado."));
            if (!"S".equals(t.getStatusAtivo()))
                throw new RegraNegocioException(CodigoErro.REGRA, "Tipo de ação inativo.");
            AcaoUsuario a = new AcaoUsuario();
            a.setIdUsuario(usuario);
            a.setIdTipoAcao(tipo);
            a.setQuantidade(quantidade);
            a.setImpactoAmbientalCalculado(Regras.calcularImpacto(quantidade, t.getImpactoAmbientalEstimado()));
            a.setPontosPrevistos(Regras.calcularPontos(a.getImpactoAmbientalCalculado(), t.getPontosBase()));
            a.setDescricaoEvidencia(ev);
            new AcaoUsuarioDAO(c).inserir(a);
            return new AcaoUsuarioDAO(c).buscar(a.getId()).orElseThrow();
        });
    }

    public List<AcaoUsuario> minhasAcoes(long usuario) throws SQLException {
        return leitura(c -> {
            ativo(c, usuario);
            List<AcaoUsuario> lista = new ArrayList<>();
            AcaoUsuarioDAO dao = new AcaoUsuarioDAO(c);
            for (long id : Jdbc.ids(c, "SELECT ID FROM ACAO_USUARIO WHERE ID_USUARIO=? ORDER BY DATA_HORA DESC", usuario))
                lista.add(dao.buscar(id).orElseThrow());
            return lista;
        });
    }

    /** Cancelamento lógico: só a própria ação, ainda pendente e sem votos. */
    public void cancelarAcao(long usuario, long acao) throws SQLException {
        transacao(c -> {
            ativo(c, usuario);
            if (Jdbc.ids(c, "SELECT ID FROM ACAO_USUARIO WHERE ID=? AND ID_USUARIO=?", acao, usuario).isEmpty())
                throw new RegraNegocioException(CodigoErro.NAO_ENCONTRADO, "Ação não encontrada.");
            if (Jdbc.numero(c, "SELECT COUNT(*) FROM VALIDACAO WHERE ID_ACAO=?", acao) > 0)
                throw new RegraNegocioException(CodigoErro.CONFLITO, "Uma ação com votos não pode ser cancelada.");
            if (Jdbc.executar(c, "UPDATE ACAO_USUARIO SET STATUS='CANCELADA',DATA_CONCLUSAO=SYSDATE WHERE ID=? AND ID_USUARIO=? AND STATUS='PENDENTE'", acao, usuario) != 1)
                throw new RegraNegocioException(CodigoErro.CONFLITO, "Ação já concluída.");
            return null;
        });
    }
}
