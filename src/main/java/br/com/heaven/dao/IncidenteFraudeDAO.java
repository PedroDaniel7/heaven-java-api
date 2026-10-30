package br.com.heaven.dao;
import br.com.heaven.model.IncidenteFraude;
import java.sql.Connection;
public class IncidenteFraudeDAO extends CrudDAO<IncidenteFraude> {
    public IncidenteFraudeDAO(Connection conexao) { super(conexao,IncidenteFraude.class); }
}
