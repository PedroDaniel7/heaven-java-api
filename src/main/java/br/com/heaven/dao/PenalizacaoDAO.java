package br.com.heaven.dao;
import br.com.heaven.model.Penalizacao;
import java.sql.Connection;
public class PenalizacaoDAO extends RegistroDAO<Penalizacao> {
    public PenalizacaoDAO(Connection conexao) { super(conexao,Penalizacao.class); }
}
