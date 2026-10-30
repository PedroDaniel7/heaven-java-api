package br.com.heaven.resource;

import br.com.heaven.bo.AcaoBO;
import br.com.heaven.config.Publico;
import br.com.heaven.dto.TipoAcaoDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.SQLException;
import java.util.List;

@RestController
@RequestMapping("/api/tipos-acao")
public class TipoAcaoResource {

    private final AcaoBO acoes;

    public TipoAcaoResource(AcaoBO acoes) {
        this.acoes = acoes;
    }

    @Publico
    @GetMapping
    public List<TipoAcaoDto> listar() throws SQLException {
        return acoes.tiposAtivos().stream().map(TipoAcaoDto::de).toList();
    }

    @Publico
    @GetMapping("/{id}")
    public TipoAcaoDto buscar(@PathVariable long id) throws SQLException {
        return TipoAcaoDto.de(acoes.tipoAtivo(id));
    }
}
