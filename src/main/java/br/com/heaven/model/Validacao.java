package br.com.heaven.model;
import br.com.heaven.persistence.Coluna;
import br.com.heaven.persistence.Tabela;
import java.math.BigDecimal;
import java.time.LocalDateTime;
/** Entidade correspondente a VALIDACAO; invariantes dos casos de uso ficam no serviço. */
@Tabela("VALIDACAO")
public class Validacao {
    @Coluna(nome="ID", chave=true, gerada=true)
    private Long id;
    @Coluna(nome="ID_ACAO", chave=false, gerada=false)
    private Long idAcao;
    @Coluna(nome="ID_VALIDADOR", chave=false, gerada=false)
    private Long idValidador;
    @Coluna(nome="VOTO", chave=false, gerada=false)
    private String voto;
    @Coluna(nome="PESO_VOTO", chave=false, gerada=false)
    private BigDecimal pesoVoto;
    @Coluna(nome="REPUTACAO_VALIDADOR_MOMENTO", chave=false, gerada=false)
    private BigDecimal reputacaoValidadorMomento;
    @Coluna(nome="DATA_HORA", chave=false, gerada=false)
    private LocalDateTime dataHora;
    @Coluna(nome="JUSTIFICATIVA", chave=false, gerada=false)
    private String justificativa;
    public Validacao() {}
    public Validacao(Long id, Long idAcao, Long idValidador, String voto, BigDecimal pesoVoto, BigDecimal reputacaoValidadorMomento, LocalDateTime dataHora, String justificativa) {
        this.id = id;
        this.idAcao = idAcao;
        this.idValidador = idValidador;
        this.voto = voto;
        this.pesoVoto = pesoVoto;
        this.reputacaoValidadorMomento = reputacaoValidadorMomento;
        this.dataHora = dataHora;
        this.justificativa = justificativa;
    }
    public Long getId() { return id; }
    public void setId(Long valor) { this.id = valor; }
    public Long getIdAcao() { return idAcao; }
    public void setIdAcao(Long valor) { this.idAcao = valor; }
    public Long getIdValidador() { return idValidador; }
    public void setIdValidador(Long valor) { this.idValidador = valor; }
    public String getVoto() { return voto; }
    public void setVoto(String valor) { this.voto = valor; }
    public BigDecimal getPesoVoto() { return pesoVoto; }
    public void setPesoVoto(BigDecimal valor) { this.pesoVoto = valor; }
    public BigDecimal getReputacaoValidadorMomento() { return reputacaoValidadorMomento; }
    public void setReputacaoValidadorMomento(BigDecimal valor) { this.reputacaoValidadorMomento = valor; }
    public LocalDateTime getDataHora() { return dataHora; }
    public void setDataHora(LocalDateTime valor) { this.dataHora = valor; }
    public String getJustificativa() { return justificativa; }
    public void setJustificativa(String valor) { this.justificativa = valor; }
}
