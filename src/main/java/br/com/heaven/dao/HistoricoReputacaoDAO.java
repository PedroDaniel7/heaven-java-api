package br.com.heaven.dao;
import br.com.heaven.model.HistoricoReputacao;
import java.sql.Connection;
public class HistoricoReputacaoDAO extends RegistroDAO<HistoricoReputacao> {
    public HistoricoReputacaoDAO(Connection conexao) { super(conexao,HistoricoReputacao.class); }
}
