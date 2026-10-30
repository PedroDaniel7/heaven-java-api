package br.com.heaven.model;
import br.com.heaven.persistence.Coluna;
import br.com.heaven.persistence.Tabela;
import java.math.BigDecimal;
/** Entidade correspondente a TIPO_ACAO; invariantes dos casos de uso ficam no serviço. */
@Tabela("TIPO_ACAO")
public class TipoAcao {
    @Coluna(nome="ID", chave=true, gerada=true)
    private Long id;
    @Coluna(nome="NOME", chave=false, gerada=false)
    private String nome;
    @Coluna(nome="DESCRICAO", chave=false, gerada=false)
    private String descricao;
    @Coluna(nome="CATEGORIA", chave=false, gerada=false)
    private String categoria;
    @Coluna(nome="UNIDADE_MEDIDA", chave=false, gerada=false)
    private String unidadeMedida;
    @Coluna(nome="IMPACTO_AMBIENTAL_ESTIMADO", chave=false, gerada=false)
    private BigDecimal impactoAmbientalEstimado;
    @Coluna(nome="PONTOS_BASE", chave=false, gerada=false)
    private Long pontosBase;
    @Coluna(nome="DIFICULDADE", chave=false, gerada=false)
    private String dificuldade;
    @Coluna(nome="STATUS_ATIVO", chave=false, gerada=false)
    private String statusAtivo;
    public TipoAcao() {}
    public TipoAcao(Long id, String nome, String descricao, String categoria, String unidadeMedida, BigDecimal impactoAmbientalEstimado, Long pontosBase, String dificuldade, String statusAtivo) {
        this.id = id;
        this.nome = nome;
        this.descricao = descricao;
        this.categoria = categoria;
        this.unidadeMedida = unidadeMedida;
        this.impactoAmbientalEstimado = impactoAmbientalEstimado;
        this.pontosBase = pontosBase;
        this.dificuldade = dificuldade;
        this.statusAtivo = statusAtivo;
    }
    public Long getId() { return id; }
    public void setId(Long valor) { this.id = valor; }
    public String getNome() { return nome; }
    public void setNome(String valor) { this.nome = valor; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String valor) { this.descricao = valor; }
    public String getCategoria() { return categoria; }
    public void setCategoria(String valor) { this.categoria = valor; }
    public String getUnidadeMedida() { return unidadeMedida; }
    public void setUnidadeMedida(String valor) { this.unidadeMedida = valor; }
    public BigDecimal getImpactoAmbientalEstimado() { return impactoAmbientalEstimado; }
    public void setImpactoAmbientalEstimado(BigDecimal valor) { this.impactoAmbientalEstimado = valor; }
    public Long getPontosBase() { return pontosBase; }
    public void setPontosBase(Long valor) { this.pontosBase = valor; }
    public String getDificuldade() { return dificuldade; }
    public void setDificuldade(String valor) { this.dificuldade = valor; }
    public String getStatusAtivo() { return statusAtivo; }
    public void setStatusAtivo(String valor) { this.statusAtivo = valor; }
}
