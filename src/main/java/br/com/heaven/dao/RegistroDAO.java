package br.com.heaven.dao;
import br.com.heaven.persistence.*;
import java.lang.reflect.Field;
import java.sql.*;
import java.util.*;
/** Escrita append-only para registros de auditoria e transações. */
public class RegistroDAO<T> extends LeituraDAO<T> {
    public RegistroDAO(Connection c,Class<T> tipo) { super(c,tipo); }
    public T inserir(T objeto) throws SQLException {
        List<Field> usados=new ArrayList<>();
        for(Field f:campos) if(!f.getAnnotation(Coluna.class).gerada() && valor(f,objeto)!=null) usados.add(f);
        if(usados.isEmpty()) throw new IllegalArgumentException("Registro sem valores");
        String sql="INSERT INTO "+tabela+" ("+String.join(",",usados.stream().map(this::nome).toList())+") VALUES ("+String.join(",",Collections.nCopies(usados.size(),"?"))+")";
        Optional<Field> gerada=campos.stream().filter(f->f.getAnnotation(Coluna.class).gerada()).findFirst();
        try(PreparedStatement s=gerada.isPresent()?conexao.prepareStatement(sql,new String[]{nome(gerada.get())}):conexao.prepareStatement(sql)) {
            for(int i=0;i<usados.size();i++) Jdbc.parametrosAt(s,i+1,valor(usados.get(i),objeto));
            s.executeUpdate();
            if(gerada.isPresent()) try(ResultSet r=s.getGeneratedKeys()) {
                if(!r.next()) throw new SQLException("Oracle não retornou a chave gerada");
                try { gerada.get().set(objeto,r.getLong(1)); } catch(IllegalAccessException e) { throw new SQLException(e); }
            }
        } return objeto;
    }
}
