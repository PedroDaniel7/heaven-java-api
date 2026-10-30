package br.com.heaven.model;
import br.com.heaven.persistence.Coluna;
import br.com.heaven.persistence.Tabela;
import java.math.BigDecimal;
import java.time.LocalDateTime;
/** Entidade correspondente a HISTORICO_PONTOS; invariantes dos casos de uso ficam no serviço. */
@Tabela("HISTORICO_PONTOS")
public class HistoricoPontos {
    @Coluna(nome="ID", chave=true, gerada=true)
    private Long id;
    @Coluna(nome="ID_USUARIO", chave=false, gerada=false)
    private Long idUsuario;
    @Coluna(nome="QUANTIDADE", chave=false, gerada=false)
    private Long quantidade;
    @Coluna(nome="TIPO_MOVIMENTACAO", chave=false, gerada=false)
    private String tipoMovimentacao;
    @Coluna(nome="DESCRICAO", chave=false, gerada=false)
    private String descricao;
    @Coluna(nome="ID_ACAO", chave=false, gerada=false)
    private Long idAcao;
    @Coluna(nome="DATA_HORA", chave=false, gerada=false)
    private LocalDateTime dataHora;
    public HistoricoPontos() {}
    public HistoricoPontos(Long id, Long idUsuario, Long quantidade, String tipoMovimentacao, String descricao, Long idAcao, LocalDateTime dataHora) {
        this.id = id;
        this.idUsuario = idUsuario;
        this.quantidade = quantidade;
        this.tipoMovimentacao = tipoMovimentacao;
        this.descricao = descricao;
        this.idAcao = idAcao;
        this.dataHora = dataHora;
    }
    public Long getId() { return id; }
    public void setId(Long valor) { this.id = valor; }
    public Long getIdUsuario() { return idUsuario; }
    public void setIdUsuario(Long valor) { this.idUsuario = valor; }
    public Long getQuantidade() { return quantidade; }
    public void setQuantidade(Long valor) { this.quantidade = valor; }
    public String getTipoMovimentacao() { return tipoMovimentacao; }
    public void setTipoMovimentacao(String valor) { this.tipoMovimentacao = valor; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String valor) { this.descricao = valor; }
    public Long getIdAcao() { return idAcao; }
    public void setIdAcao(Long valor) { this.idAcao = valor; }
    public LocalDateTime getDataHora() { return dataHora; }
    public void setDataHora(LocalDateTime valor) { this.dataHora = valor; }
}
