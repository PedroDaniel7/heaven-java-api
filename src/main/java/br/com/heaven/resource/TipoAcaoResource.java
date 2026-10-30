package br.com.heaven.resource;

import br.com.heaven.bo.AcaoBO;
import br.com.heaven.config.Publico;
import br.com.heaven.dto.TipoAcaoDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.SQLException;
import java.util.List;

@RestController
@RequestMapping("/api/tipos-acao")
@Tag(name = "Tipos de ação")
public class TipoAcaoResource {

    private final AcaoBO acoes;

    public TipoAcaoResource(AcaoBO acoes) {
        this.acoes = acoes;
    }

    @Publico
    @GetMapping
    @Operation(summary = "Lista os tipos de ação ativos")
    public List<TipoAcaoDto> listar() throws SQLException {
        return acoes.tiposAtivos().stream().map(TipoAcaoDto::de).toList();
    }

    @Publico
    @GetMapping("/{id}")
    @Operation(summary = "Busca um tipo de ação ativo")
    @RespostasDeErro({400, 404})
    public TipoAcaoDto buscar(@PathVariable long id) throws SQLException {
        return TipoAcaoDto.de(acoes.tipoAtivo(id));
    }
}
