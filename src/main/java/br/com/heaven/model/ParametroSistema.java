package br.com.heaven.model;
import br.com.heaven.persistence.Coluna;
import br.com.heaven.persistence.Tabela;
import java.math.BigDecimal;
/** Entidade correspondente a PARAMETRO_SISTEMA; invariantes dos casos de uso ficam no serviço. */
@Tabela("PARAMETRO_SISTEMA")
public class ParametroSistema {
    @Coluna(nome="ID", chave=true, gerada=true)
    private Long id;
    @Coluna(nome="NOME_PARAMETRO", chave=false, gerada=false)
    private String nomeParametro;
    @Coluna(nome="VALOR", chave=false, gerada=false)
    private BigDecimal valor;
    @Coluna(nome="DESCRICAO", chave=false, gerada=false)
    private String descricao;
    public ParametroSistema() {}
    public ParametroSistema(Long id, String nomeParametro, BigDecimal valor, String descricao) {
        this.id = id;
        this.nomeParametro = nomeParametro;
        this.valor = valor;
        this.descricao = descricao;
    }
    public Long getId() { return id; }
    public void setId(Long valor) { this.id = valor; }
    public String getNomeParametro() { return nomeParametro; }
    public void setNomeParametro(String valor) { this.nomeParametro = valor; }
    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String valor) { this.descricao = valor; }
}
