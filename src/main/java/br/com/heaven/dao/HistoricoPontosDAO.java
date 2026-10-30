package br.com.heaven.dao;
import br.com.heaven.model.HistoricoPontos;
import java.sql.Connection;
public class HistoricoPontosDAO extends RegistroDAO<HistoricoPontos> {
    public HistoricoPontosDAO(Connection conexao) { super(conexao,HistoricoPontos.class); }
}
