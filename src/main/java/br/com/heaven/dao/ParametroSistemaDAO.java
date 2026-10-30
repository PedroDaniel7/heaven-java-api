package br.com.heaven.dao;
import br.com.heaven.model.ParametroSistema;
import java.sql.Connection;
public class ParametroSistemaDAO extends CrudDAO<ParametroSistema> {
    public ParametroSistemaDAO(Connection conexao) { super(conexao,ParametroSistema.class); }
}
