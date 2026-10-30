package br.com.heaven.resource;

import br.com.heaven.bo.AcaoBO;
import br.com.heaven.bo.ResultadoVoto;
import br.com.heaven.bo.Sorteio;
import br.com.heaven.bo.ValidacaoBO;
import br.com.heaven.config.AuthInterceptor;
import br.com.heaven.dto.SorteioResposta;
import br.com.heaven.dto.VotoRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.SQLException;
import java.time.ZoneId;
import java.util.Optional;

/** Sorteio de ação e voto do validador autenticado. */
@RestController
@RequestMapping("/api/validacoes")
public class ValidacaoResource {

    private final ValidacaoBO validacoes;
    private final AcaoBO acoes;

    public ValidacaoResource(ValidacaoBO validacoes, AcaoBO acoes) {
        this.validacoes = validacoes;
        this.acoes = acoes;
    }

    @PostMapping("/sorteio")
    public ResponseEntity<SorteioResposta> sortear(@RequestAttribute(AuthInterceptor.USUARIO_ID) long usuarioId) throws SQLException {
        Optional<Sorteio> sorteio = validacoes.sortearAcao(usuarioId);
        if (sorteio.isEmpty()) return ResponseEntity.noContent().build();
        Sorteio s = sorteio.get();
        return ResponseEntity.ok(SorteioResposta.de(s.acao(), acoes.tiposPorId().get(s.acao().getIdTipoAcao()),
                s.expiraEm().atZone(ZoneId.systemDefault()).toOffsetDateTime()));
    }

    @PostMapping
    public ResponseEntity<ResultadoVoto> votar(@RequestAttribute(AuthInterceptor.USUARIO_ID) long usuarioId,
                                               @Valid @RequestBody VotoRequest corpo) throws SQLException {
        ResultadoVoto r = validacoes.votar(usuarioId, corpo.idAcao(), corpo.voto(), corpo.justificativa());
        return ResponseEntity.status(HttpStatus.CREATED).body(r);
    }
}
