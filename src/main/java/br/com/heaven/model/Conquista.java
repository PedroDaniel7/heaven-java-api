package br.com.heaven.model;
import br.com.heaven.persistence.Coluna;
import br.com.heaven.persistence.Tabela;
/** Entidade correspondente a CONQUISTA; invariantes dos casos de uso ficam no serviço. */
@Tabela("CONQUISTA")
public class Conquista {
    @Coluna(nome="ID", chave=true, gerada=true)
    private Long id;
    @Coluna(nome="NOME", chave=false, gerada=false)
    private String nome;
    @Coluna(nome="DESCRICAO", chave=false, gerada=false)
    private String descricao;
    @Coluna(nome="REQUISITO", chave=false, gerada=false)
    private String requisito;
    @Coluna(nome="PONTOS_BONUS", chave=false, gerada=false)
    private Long pontosBonus;
    public Conquista() {}
    public Conquista(Long id, String nome, String descricao, String requisito, Long pontosBonus) {
        this.id = id;
        this.nome = nome;
        this.descricao = descricao;
        this.requisito = requisito;
        this.pontosBonus = pontosBonus;
    }
    public Long getId() { return id; }
    public void setId(Long valor) { this.id = valor; }
    public String getNome() { return nome; }
    public void setNome(String valor) { this.nome = valor; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String valor) { this.descricao = valor; }
    public String getRequisito() { return requisito; }
    public void setRequisito(String valor) { this.requisito = valor; }
    public Long getPontosBonus() { return pontosBonus; }
    public void setPontosBonus(Long valor) { this.pontosBonus = valor; }
}
