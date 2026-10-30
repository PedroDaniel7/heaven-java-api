package br.com.heaven.dao;
import br.com.heaven.model.AcaoUsuario;
import java.sql.Connection;
public class AcaoUsuarioDAO extends RegistroDAO<AcaoUsuario> {
    public AcaoUsuarioDAO(Connection conexao) { super(conexao,AcaoUsuario.class); }
}
