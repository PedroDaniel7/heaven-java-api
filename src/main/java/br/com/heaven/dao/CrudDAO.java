package br.com.heaven.dao;
import br.com.heaven.persistence.*;
import java.lang.reflect.Field;
import java.sql.*;
import java.util.*;
/** CRUD dos cadastros. O chamador controla commit/rollback. FKs impedem exclusão em uso. */
public class CrudDAO<T> extends RegistroDAO<T> {
    public CrudDAO(Connection c,Class<T> tipo) { super(c,tipo); }
    protected boolean editavel(Field f) { return !f.getAnnotation(Coluna.class).chave(); }
    public void atualizar(T objeto) throws SQLException {
        List<Field> usados=campos.stream().filter(this::editavel).toList();
        String sql="UPDATE "+tabela+" SET "+String.join(",",usados.stream().map(f->nome(f)+"=?").toList())+" WHERE "+filtroChaves();
        try(PreparedStatement s=conexao.prepareStatement(sql)) {
            int i=1; for(Field f:usados) Jdbc.parametrosAt(s,i++,valor(f,objeto));
            for(Field f:chaves()) Jdbc.parametrosAt(s,i++,valor(f,objeto));
            if(s.executeUpdate()!=1) throw new SQLException("Registro não encontrado para atualização");
        }
    }
    public boolean excluir(Object... ids) throws SQLException {
        if(ids.length!=chaves().size()) throw new IllegalArgumentException("Quantidade incorreta de chaves");
        return Jdbc.executar(conexao,"DELETE FROM "+tabela+" WHERE "+filtroChaves(),ids)==1;
    }
}
