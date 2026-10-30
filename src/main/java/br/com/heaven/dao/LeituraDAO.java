package br.com.heaven.dao;
import br.com.heaven.persistence.*;
import java.lang.reflect.*;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
/** Metadados vêm apenas das classes internas; valores sempre usam bind parameters. */
public class LeituraDAO<T> {
    protected final Connection conexao;
    protected final Class<T> tipo;
    protected final String tabela;
    protected final List<Field> campos;
    public LeituraDAO(Connection conexao, Class<T> tipo) {
        this.conexao=Objects.requireNonNull(conexao); this.tipo=tipo;
        tabela=tipo.getAnnotation(Tabela.class).value();
        campos=Arrays.stream(tipo.getDeclaredFields()).filter(f->f.isAnnotationPresent(Coluna.class)).toList();
        campos.forEach(f->f.setAccessible(true));
    }
    protected String nome(Field f) { return f.getAnnotation(Coluna.class).nome(); }
    protected List<Field> chaves() { return campos.stream().filter(f->f.getAnnotation(Coluna.class).chave()).toList(); }
    protected String filtroChaves() { return String.join(" AND ", chaves().stream().map(f->nome(f)+"=?").toList()); }
    protected Object valor(Field f,T objeto) throws SQLException {
        try { return f.get(objeto); } catch(IllegalAccessException e) { throw new SQLException(e); }
    }
    protected T mapear(ResultSet r) throws SQLException {
        try {
            T obj=tipo.getDeclaredConstructor().newInstance();
            for(Field f:campos) {
                Object v;
                if(f.getType()==Long.class) { long n=r.getLong(nome(f)); v=r.wasNull()?null:n; }
                else if(f.getType()==BigDecimal.class) v=r.getBigDecimal(nome(f));
                else if(f.getType()==LocalDateTime.class) { Timestamp t=r.getTimestamp(nome(f)); v=t==null?null:t.toLocalDateTime(); }
                else v=r.getString(nome(f));
                f.set(obj,v);
            } return obj;
        } catch(ReflectiveOperationException e) { throw new SQLException("Falha no mapeamento "+tipo.getSimpleName(),e); }
    }
    public Optional<T> buscar(Object... ids) throws SQLException {
        if(ids.length!=chaves().size()) throw new IllegalArgumentException("Quantidade incorreta de chaves");
        try(PreparedStatement s=conexao.prepareStatement("SELECT * FROM "+tabela+" WHERE "+filtroChaves())) {
            Jdbc.parametros(s,ids); try(ResultSet r=s.executeQuery()) { return r.next()?Optional.of(mapear(r)):Optional.empty(); }
        }
    }
    public List<T> listar() throws SQLException {
        List<T> lista=new ArrayList<>();
        try(PreparedStatement s=conexao.prepareStatement("SELECT * FROM "+tabela+" ORDER BY "+String.join(",",chaves().stream().map(this::nome).toList())); ResultSet r=s.executeQuery()) {
            while(r.next()) lista.add(mapear(r));
        } return lista;
    }
}
