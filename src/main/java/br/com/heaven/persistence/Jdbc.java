package br.com.heaven.persistence;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
public final class Jdbc {
    private Jdbc() {}
    public static void parametros(PreparedStatement s, Object... valores) throws SQLException {
        for (int i=0; i<valores.length; i++) {
            Object v=valores[i];
            if (v instanceof LocalDateTime d) s.setTimestamp(i+1, Timestamp.valueOf(d));
            else s.setObject(i+1,v);
        }
    }
    public static void parametrosAt(PreparedStatement s,int indice,Object v) throws SQLException {
        if(v instanceof LocalDateTime d) s.setTimestamp(indice,Timestamp.valueOf(d)); else s.setObject(indice,v);
    }
    public static int executar(Connection c,String sql,Object... args) throws SQLException {
        try (PreparedStatement s=c.prepareStatement(sql)) { parametros(s,args); return s.executeUpdate(); }
    }
    public static long numero(Connection c,String sql,Object... args) throws SQLException {
        try (PreparedStatement s=c.prepareStatement(sql)) {
            parametros(s,args); try(ResultSet r=s.executeQuery()) {
                if(!r.next()) throw new SQLException("Registro obrigatório ausente."); return r.getLong(1);
            }
        }
    }
    public static List<Long> ids(Connection c,String sql,Object... args) throws SQLException {
        List<Long> lista=new ArrayList<>();
        try(PreparedStatement s=c.prepareStatement(sql)) {
            parametros(s,args); try(ResultSet r=s.executeQuery()) { while(r.next()) lista.add(r.getLong(1)); }
        } return lista;
    }
}
