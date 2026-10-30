package br.com.heaven.dao;
import br.com.heaven.model.TipoAcao;
import java.sql.Connection;
public class TipoAcaoDAO extends CrudDAO<TipoAcao> {
    public TipoAcaoDAO(Connection conexao) { super(conexao,TipoAcao.class); }
}
