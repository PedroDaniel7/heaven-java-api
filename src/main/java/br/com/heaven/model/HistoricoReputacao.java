package br.com.heaven.model;
import br.com.heaven.persistence.Coluna;
import br.com.heaven.persistence.Tabela;
import java.math.BigDecimal;
import java.time.LocalDateTime;
/** Entidade correspondente a HISTORICO_REPUTACAO; invariantes dos casos de uso ficam no serviço. */
@Tabela("HISTORICO_REPUTACAO")
public class HistoricoReputacao {
    @Coluna(nome="ID", chave=true, gerada=true)
    private Long id;
    @Coluna(nome="ID_USUARIO", chave=false, gerada=false)
    private Long idUsuario;
    @Coluna(nome="REPUTACAO_ANTERIOR", chave=false, gerada=false)
    private BigDecimal reputacaoAnterior;
    @Coluna(nome="REPUTACAO_NOVA", chave=false, gerada=false)
    private BigDecimal reputacaoNova;
    @Coluna(nome="MOTIVO", chave=false, gerada=false)
    private String motivo;
    @Coluna(nome="ID_VALIDACAO", chave=false, gerada=false)
    private Long idValidacao;
    @Coluna(nome="DATA_HORA", chave=false, gerada=false)
    private LocalDateTime dataHora;
    public HistoricoReputacao() {}
    public HistoricoReputacao(Long id, Long idUsuario, BigDecimal reputacaoAnterior, BigDecimal reputacaoNova, String motivo, Long idValidacao, LocalDateTime dataHora) {
        this.id = id;
        this.idUsuario = idUsuario;
        this.reputacaoAnterior = reputacaoAnterior;
        this.reputacaoNova = reputacaoNova;
        this.motivo = motivo;
        this.idValidacao = idValidacao;
        this.dataHora = dataHora;
    }
    public Long getId() { return id; }
    public void setId(Long valor) { this.id = valor; }
    public Long getIdUsuario() { return idUsuario; }
    public void setIdUsuario(Long valor) { this.idUsuario = valor; }
    public BigDecimal getReputacaoAnterior() { return reputacaoAnterior; }
    public void setReputacaoAnterior(BigDecimal valor) { this.reputacaoAnterior = valor; }
    public BigDecimal getReputacaoNova() { return reputacaoNova; }
    public void setReputacaoNova(BigDecimal valor) { this.reputacaoNova = valor; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String valor) { this.motivo = valor; }
    public Long getIdValidacao() { return idValidacao; }
    public void setIdValidacao(Long valor) { this.idValidacao = valor; }
    public LocalDateTime getDataHora() { return dataHora; }
    public void setDataHora(LocalDateTime valor) { this.dataHora = valor; }
}
