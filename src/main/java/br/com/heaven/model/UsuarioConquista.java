package br.com.heaven.model;
import br.com.heaven.persistence.Coluna;
import br.com.heaven.persistence.Tabela;
import java.time.LocalDateTime;
/** Entidade correspondente a USUARIO_CONQUISTA; invariantes dos casos de uso ficam no serviço. */
@Tabela("USUARIO_CONQUISTA")
public class UsuarioConquista {
    @Coluna(nome="ID_USUARIO", chave=true, gerada=false)
    private Long idUsuario;
    @Coluna(nome="ID_CONQUISTA", chave=true, gerada=false)
    private Long idConquista;
    @Coluna(nome="DATA_CONQUISTA", chave=false, gerada=false)
    private LocalDateTime dataConquista;
    public UsuarioConquista() {}
    public UsuarioConquista(Long idUsuario, Long idConquista, LocalDateTime dataConquista) {
        this.idUsuario = idUsuario;
        this.idConquista = idConquista;
        this.dataConquista = dataConquista;
    }
    public Long getIdUsuario() { return idUsuario; }
    public void setIdUsuario(Long valor) { this.idUsuario = valor; }
    public Long getIdConquista() { return idConquista; }
    public void setIdConquista(Long valor) { this.idConquista = valor; }
    public LocalDateTime getDataConquista() { return dataConquista; }
    public void setDataConquista(LocalDateTime valor) { this.dataConquista = valor; }
}
