package br.com.heaven.model;
import br.com.heaven.persistence.Coluna;
import br.com.heaven.persistence.Tabela;
import java.math.BigDecimal;
import java.time.LocalDateTime;
/** Entidade correspondente a USUARIO; invariantes dos casos de uso ficam no serviço. */
@Tabela("USUARIO")
public class Usuario {
    @Coluna(nome="ID", chave=true, gerada=true)
    private Long id;
    @Coluna(nome="NOME", chave=false, gerada=false)
    private String nome;
    @Coluna(nome="EMAIL", chave=false, gerada=false)
    private String email;
    @Coluna(nome="SENHA_HASH", chave=false, gerada=false)
    private String senhaHash;
    @Coluna(nome="DATA_CADASTRO", chave=false, gerada=false)
    private LocalDateTime dataCadastro;
    @Coluna(nome="STATUS_CONTA", chave=false, gerada=false)
    private String statusConta;
    @Coluna(nome="PONTOS_ATUAIS", chave=false, gerada=false)
    private Long pontosAtuais;
    @Coluna(nome="REPUTACAO_ATUAL", chave=false, gerada=false)
    private BigDecimal reputacaoAtual;
    @Coluna(nome="ID_NIVEL_CONFIANCA", chave=false, gerada=false)
    private Long idNivelConfianca;
    @Coluna(nome="STREAK_ATUAL", chave=false, gerada=false)
    private Long streakAtual;
    @Coluna(nome="MAIOR_STREAK", chave=false, gerada=false)
    private Long maiorStreak;
    @Coluna(nome="DATA_ULTIMA_ACAO", chave=false, gerada=false)
    private LocalDateTime dataUltimaAcao;
    public Usuario() {}
    public Usuario(Long id, String nome, String email, String senhaHash, LocalDateTime dataCadastro, String statusConta, Long pontosAtuais, BigDecimal reputacaoAtual, Long idNivelConfianca, Long streakAtual, Long maiorStreak, LocalDateTime dataUltimaAcao) {
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.senhaHash = senhaHash;
        this.dataCadastro = dataCadastro;
        this.statusConta = statusConta;
        this.pontosAtuais = pontosAtuais;
        this.reputacaoAtual = reputacaoAtual;
        this.idNivelConfianca = idNivelConfianca;
        this.streakAtual = streakAtual;
        this.maiorStreak = maiorStreak;
        this.dataUltimaAcao = dataUltimaAcao;
    }
    public Long getId() { return id; }
    public void setId(Long valor) { this.id = valor; }
    public String getNome() { return nome; }
    public void setNome(String valor) { this.nome = valor; }
    public String getEmail() { return email; }
    public void setEmail(String valor) { this.email = valor; }
    public String getSenhaHash() { return senhaHash; }
    public void setSenhaHash(String valor) { this.senhaHash = valor; }
    public LocalDateTime getDataCadastro() { return dataCadastro; }
    public void setDataCadastro(LocalDateTime valor) { this.dataCadastro = valor; }
    public String getStatusConta() { return statusConta; }
    public void setStatusConta(String valor) { this.statusConta = valor; }
    public Long getPontosAtuais() { return pontosAtuais; }
    public void setPontosAtuais(Long valor) { this.pontosAtuais = valor; }
    public BigDecimal getReputacaoAtual() { return reputacaoAtual; }
    public void setReputacaoAtual(BigDecimal valor) { this.reputacaoAtual = valor; }
    public Long getIdNivelConfianca() { return idNivelConfianca; }
    public void setIdNivelConfianca(Long valor) { this.idNivelConfianca = valor; }
    public Long getStreakAtual() { return streakAtual; }
    public void setStreakAtual(Long valor) { this.streakAtual = valor; }
    public Long getMaiorStreak() { return maiorStreak; }
    public void setMaiorStreak(Long valor) { this.maiorStreak = valor; }
    public LocalDateTime getDataUltimaAcao() { return dataUltimaAcao; }
    public void setDataUltimaAcao(LocalDateTime valor) { this.dataUltimaAcao = valor; }
}
