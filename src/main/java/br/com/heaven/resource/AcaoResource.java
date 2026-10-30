package br.com.heaven.resource;

import br.com.heaven.bo.AcaoBO;
import br.com.heaven.config.AuthInterceptor;
import br.com.heaven.dto.AcaoDto;
import br.com.heaven.dto.AcaoRequest;
import br.com.heaven.model.AcaoUsuario;
import br.com.heaven.model.TipoAcao;
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
public class AcaoResource {

    private final AcaoBO acoes;

    public AcaoResource(AcaoBO acoes) {
        this.acoes = acoes;
    }

    @PostMapping
    public ResponseEntity<AcaoDto> registrar(@RequestAttribute(AuthInterceptor.USUARIO_ID) long usuarioId,
                                             @Valid @RequestBody AcaoRequest corpo) throws SQLException {
        AcaoUsuario a = acoes.registrarAcao(usuarioId, corpo.idTipoAcao(), corpo.quantidade(), corpo.evidencia());
        return ResponseEntity.created(URI.create("/api/acoes/" + a.getId())).body(dto(a, acoes.tiposPorId()));
    }

    @GetMapping("/minhas")
    public List<AcaoDto> minhas(@RequestAttribute(AuthInterceptor.USUARIO_ID) long usuarioId) throws SQLException {
        List<AcaoUsuario> lista = acoes.minhasAcoes(usuarioId);
        Map<Long, TipoAcao> tipos = acoes.tiposPorId();
        return lista.stream().map(a -> dto(a, tipos)).toList();
    }

    @GetMapping("/{id}")
    public AcaoDto buscar(@RequestAttribute(AuthInterceptor.USUARIO_ID) long usuarioId, @PathVariable long id) throws SQLException {
        return dto(acoes.buscarMinha(usuarioId, id), acoes.tiposPorId());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelar(@RequestAttribute(AuthInterceptor.USUARIO_ID) long usuarioId, @PathVariable long id) throws SQLException {
        acoes.cancelarAcao(usuarioId, id);
        return ResponseEntity.noContent().build();
    }

    private static AcaoDto dto(AcaoUsuario a, Map<Long, TipoAcao> tipos) {
        return AcaoDto.de(a, tipos.get(a.getIdTipoAcao()));
    }
}
