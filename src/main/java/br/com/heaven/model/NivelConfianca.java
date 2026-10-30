package br.com.heaven.model;
import br.com.heaven.persistence.Coluna;
import br.com.heaven.persistence.Tabela;
import java.math.BigDecimal;
import java.time.LocalDateTime;
/** Entidade correspondente a NIVEL_CONFIANCA; invariantes dos casos de uso ficam no serviço. */
@Tabela("NIVEL_CONFIANCA")
public class NivelConfianca {
    @Coluna(nome="ID", chave=true, gerada=true)
    private Long id;
    @Coluna(nome="NOME", chave=false, gerada=false)
    private String nome;
    @Coluna(nome="REPUTACAO_MINIMA", chave=false, gerada=false)
    private BigDecimal reputacaoMinima;
    @Coluna(nome="PESO_VOTO", chave=false, gerada=false)
    private BigDecimal pesoVoto;
    @Coluna(nome="LIMITE_VALIDACOES_DIA", chave=false, gerada=false)
    private Long limiteValidacoesDia;
    @Coluna(nome="LIMITE_ACOES_DIA", chave=false, gerada=false)
    private Long limiteAcoesDia;
    public NivelConfianca() {}
    public NivelConfianca(Long id, String nome, BigDecimal reputacaoMinima, BigDecimal pesoVoto, Long limiteValidacoesDia, Long limiteAcoesDia) {
        this.id = id;
        this.nome = nome;
        this.reputacaoMinima = reputacaoMinima;
        this.pesoVoto = pesoVoto;
        this.limiteValidacoesDia = limiteValidacoesDia;
        this.limiteAcoesDia = limiteAcoesDia;
    }
    public Long getId() { return id; }
    public void setId(Long valor) { this.id = valor; }
    public String getNome() { return nome; }
    public void setNome(String valor) { this.nome = valor; }
    public BigDecimal getReputacaoMinima() { return reputacaoMinima; }
    public void setReputacaoMinima(BigDecimal valor) { this.reputacaoMinima = valor; }
    public BigDecimal getPesoVoto() { return pesoVoto; }
    public void setPesoVoto(BigDecimal valor) { this.pesoVoto = valor; }
    public Long getLimiteValidacoesDia() { return limiteValidacoesDia; }
    public void setLimiteValidacoesDia(Long valor) { this.limiteValidacoesDia = valor; }
    public Long getLimiteAcoesDia() { return limiteAcoesDia; }
    public void setLimiteAcoesDia(Long valor) { this.limiteAcoesDia = valor; }
}
