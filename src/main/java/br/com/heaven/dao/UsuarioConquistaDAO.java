package br.com.heaven.dao;
import br.com.heaven.model.UsuarioConquista;
import java.sql.Connection;
public class UsuarioConquistaDAO extends RegistroDAO<UsuarioConquista> {
    public UsuarioConquistaDAO(Connection conexao) { super(conexao,UsuarioConquista.class); }
}
