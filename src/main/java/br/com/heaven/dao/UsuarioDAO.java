package br.com.heaven.dao;
import br.com.heaven.model.Usuario;
import java.sql.Connection;
public class UsuarioDAO extends CrudDAO<Usuario> {
    public UsuarioDAO(Connection conexao) { super(conexao,Usuario.class); }

    @Override protected boolean editavel(java.lang.reflect.Field f) {
        return java.util.Set.of("nome","email","senhaHash","statusConta").contains(f.getName());
    }
}
