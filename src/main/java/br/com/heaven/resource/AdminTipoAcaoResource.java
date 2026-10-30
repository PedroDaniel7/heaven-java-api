package br.com.heaven.resource;

import br.com.heaven.bo.TipoAcaoAdminBO;
import br.com.heaven.config.OpenApiConfig;
import br.com.heaven.config.SomenteAdmin;
import br.com.heaven.dto.TipoAcaoAdminDto;
import br.com.heaven.dto.TipoAcaoAdminRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.SQLException;
import java.util.List;

/** Administração de tipos de ação, protegida pelo header X-Admin-Key. */
@RestController
@RequestMapping("/api/admin/tipos-acao")
@SomenteAdmin
@Tag(name = "Administração")
@SecurityRequirement(name = OpenApiConfig.CHAVE_ADMIN)
public class AdminTipoAcaoResource {

    private final TipoAcaoAdminBO tipos;

    public AdminTipoAcaoResource(TipoAcaoAdminBO tipos) {
        this.tipos = tipos;
    }

    @GetMapping
    @Operation(summary = "Lista todos os tipos, inclusive inativos")
    @RespostasDeErro({401})
    public List<TipoAcaoAdminDto> listar() throws SQLException {
        return tipos.listar().stream().map(TipoAcaoAdminDto::de).toList();
    }

    @PostMapping
    @Operation(summary = "Cria um tipo de ação",
            description = "Nome repetido devolve 409.")
    @RespostasDeErro(value = {400, 401, 409}, sucesso = 201)
    public ResponseEntity<TipoAcaoAdminDto> criar(@Valid @RequestBody TipoAcaoAdminRequest corpo) throws SQLException {
        return ResponseEntity.status(HttpStatus.CREATED).body(TipoAcaoAdminDto.de(tipos.criar(dados(corpo))));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza um tipo de ação",
            description = "ativo ausente mantém o valor atual.")
    @RespostasDeErro({400, 401, 404, 409})
    public TipoAcaoAdminDto atualizar(@PathVariable long id, @Valid @RequestBody TipoAcaoAdminRequest corpo) throws SQLException {
        return TipoAcaoAdminDto.de(tipos.atualizar(id, dados(corpo)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Apaga um tipo de ação",
            description = "Tipo com ações vinculadas devolve 409; desative-o em vez de apagar.")
    @RespostasDeErro(value = {401, 404, 409}, sucesso = 204)
    public ResponseEntity<Void> excluir(@PathVariable long id) throws SQLException {
        tipos.excluir(id);
        return ResponseEntity.noContent().build();
    }

    private static TipoAcaoAdminBO.Dados dados(TipoAcaoAdminRequest r) {
        return new TipoAcaoAdminBO.Dados(r.nome(), r.desafio(), r.categoria(), r.unidade(),
                r.impactoAmbientalEstimado(), r.pontosBase(), r.dificuldade(), r.ativo());
    }
}
