package br.com.heaven.model;
import br.com.heaven.persistence.Coluna;
import br.com.heaven.persistence.Tabela;
import java.math.BigDecimal;
import java.time.LocalDateTime;
/** Entidade correspondente a PENALIZACAO; invariantes dos casos de uso ficam no serviço. */
@Tabela("PENALIZACAO")
public class Penalizacao {
    @Coluna(nome="ID", chave=true, gerada=true)
    private Long id;
    @Coluna(nome="ID_USUARIO", chave=false, gerada=false)
    private Long idUsuario;
    @Coluna(nome="ID_INCIDENTE", chave=false, gerada=false)
    private Long idIncidente;
    @Coluna(nome="TIPO_PENALIZACAO", chave=false, gerada=false)
    private String tipoPenalizacao;
    @Coluna(nome="DESCRICAO", chave=false, gerada=false)
    private String descricao;
    @Coluna(nome="PONTOS_PERDIDOS", chave=false, gerada=false)
    private Long pontosPerdidos;
    @Coluna(nome="REPUTACAO_REDUZIDA", chave=false, gerada=false)
    private BigDecimal reputacaoReduzida;
    @Coluna(nome="DATA_INICIO", chave=false, gerada=false)
    private LocalDateTime dataInicio;
    @Coluna(nome="DATA_FIM", chave=false, gerada=false)
    private LocalDateTime dataFim;
    @Coluna(nome="STATUS", chave=false, gerada=false)
    private String status;
    public Penalizacao() {}
    public Penalizacao(Long id, Long idUsuario, Long idIncidente, String tipoPenalizacao, String descricao, Long pontosPerdidos, BigDecimal reputacaoReduzida, LocalDateTime dataInicio, LocalDateTime dataFim, String status) {
        this.id = id;
        this.idUsuario = idUsuario;
        this.idIncidente = idIncidente;
        this.tipoPenalizacao = tipoPenalizacao;
        this.descricao = descricao;
        this.pontosPerdidos = pontosPerdidos;
        this.reputacaoReduzida = reputacaoReduzida;
        this.dataInicio = dataInicio;
        this.dataFim = dataFim;
        this.status = status;
    }
    public Long getId() { return id; }
    public void setId(Long valor) { this.id = valor; }
    public Long getIdUsuario() { return idUsuario; }
    public void setIdUsuario(Long valor) { this.idUsuario = valor; }
    public Long getIdIncidente() { return idIncidente; }
    public void setIdIncidente(Long valor) { this.idIncidente = valor; }
    public String getTipoPenalizacao() { return tipoPenalizacao; }
    public void setTipoPenalizacao(String valor) { this.tipoPenalizacao = valor; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String valor) { this.descricao = valor; }
    public Long getPontosPerdidos() { return pontosPerdidos; }
    public void setPontosPerdidos(Long valor) { this.pontosPerdidos = valor; }
    public BigDecimal getReputacaoReduzida() { return reputacaoReduzida; }
    public void setReputacaoReduzida(BigDecimal valor) { this.reputacaoReduzida = valor; }
    public LocalDateTime getDataInicio() { return dataInicio; }
    public void setDataInicio(LocalDateTime valor) { this.dataInicio = valor; }
    public LocalDateTime getDataFim() { return dataFim; }
    public void setDataFim(LocalDateTime valor) { this.dataFim = valor; }
    public String getStatus() { return status; }
    public void setStatus(String valor) { this.status = valor; }
}
