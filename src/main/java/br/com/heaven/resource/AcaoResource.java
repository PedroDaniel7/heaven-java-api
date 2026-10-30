package br.com.heaven.resource;

import br.com.heaven.bo.AcaoBO;
import br.com.heaven.config.AuthInterceptor;
import br.com.heaven.config.OpenApiConfig;
import br.com.heaven.dto.AcaoDto;
import br.com.heaven.dto.AcaoRequest;
import br.com.heaven.model.AcaoUsuario;
import br.com.heaven.model.TipoAcao;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/** Ações sustentáveis do usuário autenticado. */
@RestController
@RequestMapping("/api/acoes")
@Tag(name = "Ações")
@SecurityRequirement(name = OpenApiConfig.BEARER)
public class AcaoResource {

    private final AcaoBO acoes;

    public AcaoResource(AcaoBO acoes) {
        this.acoes = acoes;
    }

    @PostMapping
    @Operation(summary = "Registra uma ação",
            description = "Nasce PENDENTE. 422 por limite diário ou tipo inativo; 403 se houver bloqueio.")
    @RespostasDeErro(value = {400, 401, 403, 404, 422}, sucesso = 201)
    public ResponseEntity<AcaoDto> registrar(@RequestAttribute(AuthInterceptor.USUARIO_ID) long usuarioId,
                                             @Valid @RequestBody AcaoRequest corpo) throws SQLException {
        AcaoUsuario a = acoes.registrarAcao(usuarioId, corpo.idTipoAcao(), corpo.quantidade(), corpo.evidencia());
        return ResponseEntity.created(URI.create("/api/acoes/" + a.getId())).body(dto(a, acoes.tiposPorId()));
    }

    @GetMapping("/minhas")
    @Operation(summary = "Lista as ações do usuário",
            description = "Mais recentes primeiro.")
    @RespostasDeErro({401})
    public List<AcaoDto> minhas(@RequestAttribute(AuthInterceptor.USUARIO_ID) long usuarioId) throws SQLException {
        List<AcaoUsuario> lista = acoes.minhasAcoes(usuarioId);
        Map<Long, TipoAcao> tipos = acoes.tiposPorId();
        return lista.stream().map(a -> dto(a, tipos)).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca uma ação do usuário",
            description = "Ação de outro usuário também devolve 404.")
    @RespostasDeErro({401, 404})
    public AcaoDto buscar(@RequestAttribute(AuthInterceptor.USUARIO_ID) long usuarioId, @PathVariable long id) throws SQLException {
        return dto(acoes.buscarMinha(usuarioId, id), acoes.tiposPorId());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Cancela uma ação",
            description = "Cancelamento lógico (CANCELADA). 409 se já concluída ou com votos.")
    @RespostasDeErro(value = {401, 404, 409}, sucesso = 204)
    public ResponseEntity<Void> cancelar(@RequestAttribute(AuthInterceptor.USUARIO_ID) long usuarioId, @PathVariable long id) throws SQLException {
        acoes.cancelarAcao(usuarioId, id);
        return ResponseEntity.noContent().build();
    }

    private static AcaoDto dto(AcaoUsuario a, Map<Long, TipoAcao> tipos) {
        return AcaoDto.de(a, tipos.get(a.getIdTipoAcao()));
    }
}
