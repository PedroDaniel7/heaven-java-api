package br.com.heaven.dao;
import br.com.heaven.model.Validacao;
import java.sql.Connection;
public class ValidacaoDAO extends RegistroDAO<Validacao> {
    public ValidacaoDAO(Connection conexao) { super(conexao,Validacao.class); }
}
