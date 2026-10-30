package br.com.heaven.dao;
import br.com.heaven.model.NivelConfianca;
import java.sql.Connection;
public class NivelConfiancaDAO extends CrudDAO<NivelConfianca> {
    public NivelConfiancaDAO(Connection conexao) { super(conexao,NivelConfianca.class); }
}
