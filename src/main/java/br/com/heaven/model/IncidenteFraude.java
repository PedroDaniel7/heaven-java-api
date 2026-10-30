package br.com.heaven.model;
import br.com.heaven.persistence.Coluna;
import br.com.heaven.persistence.Tabela;
import java.math.BigDecimal;
import java.time.LocalDateTime;
/** Entidade correspondente a INCIDENTE_FRAUDE; invariantes dos casos de uso ficam no serviço. */
@Tabela("INCIDENTE_FRAUDE")
public class IncidenteFraude {
    @Coluna(nome="ID", chave=true, gerada=true)
    private Long id;
    @Coluna(nome="ID_USUARIO", chave=false, gerada=false)
    private Long idUsuario;
    @Coluna(nome="TIPO_INCIDENTE", chave=false, gerada=false)
    private String tipoIncidente;
    @Coluna(nome="DESCRICAO", chave=false, gerada=false)
    private String descricao;
    @Coluna(nome="NIVEL_SEVERIDADE", chave=false, gerada=false)
    private String nivelSeveridade;
    @Coluna(nome="DATA_HORA", chave=false, gerada=false)
    private LocalDateTime dataHora;
    @Coluna(nome="STATUS", chave=false, gerada=false)
    private String status;
    public IncidenteFraude() {}
    public IncidenteFraude(Long id, Long idUsuario, String tipoIncidente, String descricao, String nivelSeveridade, LocalDateTime dataHora, String status) {
        this.id = id;
        this.idUsuario = idUsuario;
        this.tipoIncidente = tipoIncidente;
        this.descricao = descricao;
        this.nivelSeveridade = nivelSeveridade;
        this.dataHora = dataHora;
        this.status = status;
    }
    public Long getId() { return id; }
    public void setId(Long valor) { this.id = valor; }
    public Long getIdUsuario() { return idUsuario; }
    public void setIdUsuario(Long valor) { this.idUsuario = valor; }
    public String getTipoIncidente() { return tipoIncidente; }
    public void setTipoIncidente(String valor) { this.tipoIncidente = valor; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String valor) { this.descricao = valor; }
    public String getNivelSeveridade() { return nivelSeveridade; }
    public void setNivelSeveridade(String valor) { this.nivelSeveridade = valor; }
    public LocalDateTime getDataHora() { return dataHora; }
    public void setDataHora(LocalDateTime valor) { this.dataHora = valor; }
    public String getStatus() { return status; }
    public void setStatus(String valor) { this.status = valor; }
}
