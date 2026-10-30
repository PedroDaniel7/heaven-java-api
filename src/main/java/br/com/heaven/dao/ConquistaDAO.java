package br.com.heaven.dao;
import br.com.heaven.model.Conquista;
import java.sql.Connection;
public class ConquistaDAO extends CrudDAO<Conquista> {
    public ConquistaDAO(Connection conexao) { super(conexao,Conquista.class); }
}
