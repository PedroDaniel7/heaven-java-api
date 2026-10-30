package br.com.heaven.model;
import br.com.heaven.persistence.Coluna;
import br.com.heaven.persistence.Tabela;
import java.math.BigDecimal;
import java.time.LocalDateTime;
/** Entidade correspondente a ACAO_USUARIO; invariantes dos casos de uso ficam no serviço. */
@Tabela("ACAO_USUARIO")
public class AcaoUsuario {
    @Coluna(nome="ID", chave=true, gerada=true)
    private Long id;
    @Coluna(nome="ID_USUARIO", chave=false, gerada=false)
    private Long idUsuario;
    @Coluna(nome="ID_TIPO_ACAO", chave=false, gerada=false)
    private Long idTipoAcao;
    @Coluna(nome="DATA_HORA", chave=false, gerada=false)
    private LocalDateTime dataHora;
    @Coluna(nome="QUANTIDADE", chave=false, gerada=false)
    private BigDecimal quantidade;
    @Coluna(nome="IMPACTO_AMBIENTAL_CALCULADO", chave=false, gerada=false)
    private BigDecimal impactoAmbientalCalculado;
    @Coluna(nome="PONTOS_PREVISTOS", chave=false, gerada=false)
    private Long pontosPrevistos;
    @Coluna(nome="STATUS", chave=false, gerada=false)
    private String status;
    @Coluna(nome="DESCRICAO_EVIDENCIA", chave=false, gerada=false)
    private String descricaoEvidencia;
    @Coluna(nome="DATA_CONCLUSAO", chave=false, gerada=false)
    private LocalDateTime dataConclusao;
    public AcaoUsuario() {}
    public AcaoUsuario(Long id, Long idUsuario, Long idTipoAcao, LocalDateTime dataHora, BigDecimal quantidade, BigDecimal impactoAmbientalCalculado, Long pontosPrevistos, String status, String descricaoEvidencia, LocalDateTime dataConclusao) {
        this.id = id;
        this.idUsuario = idUsuario;
        this.idTipoAcao = idTipoAcao;
        this.dataHora = dataHora;
        this.quantidade = quantidade;
        this.impactoAmbientalCalculado = impactoAmbientalCalculado;
        this.pontosPrevistos = pontosPrevistos;
        this.status = status;
        this.descricaoEvidencia = descricaoEvidencia;
        this.dataConclusao = dataConclusao;
    }
    public Long getId() { return id; }
    public void setId(Long valor) { this.id = valor; }
    public Long getIdUsuario() { return idUsuario; }
    public void setIdUsuario(Long valor) { this.idUsuario = valor; }
    public Long getIdTipoAcao() { return idTipoAcao; }
    public void setIdTipoAcao(Long valor) { this.idTipoAcao = valor; }
    public LocalDateTime getDataHora() { return dataHora; }
    public void setDataHora(LocalDateTime valor) { this.dataHora = valor; }
    public BigDecimal getQuantidade() { return quantidade; }
    public void setQuantidade(BigDecimal valor) { this.quantidade = valor; }
    public BigDecimal getImpactoAmbientalCalculado() { return impactoAmbientalCalculado; }
    public void setImpactoAmbientalCalculado(BigDecimal valor) { this.impactoAmbientalCalculado = valor; }
    public Long getPontosPrevistos() { return pontosPrevistos; }
    public void setPontosPrevistos(Long valor) { this.pontosPrevistos = valor; }
    public String getStatus() { return status; }
    public void setStatus(String valor) { this.status = valor; }
    public String getDescricaoEvidencia() { return descricaoEvidencia; }
    public void setDescricaoEvidencia(String valor) { this.descricaoEvidencia = valor; }
    public LocalDateTime getDataConclusao() { return dataConclusao; }
    public void setDataConclusao(LocalDateTime valor) { this.dataConclusao = valor; }
}
