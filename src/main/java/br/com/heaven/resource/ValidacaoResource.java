package br.com.heaven.resource;

import br.com.heaven.bo.AcaoBO;
import br.com.heaven.bo.ResultadoVoto;
import br.com.heaven.bo.Sorteio;
import br.com.heaven.bo.ValidacaoBO;
import br.com.heaven.config.AuthInterceptor;
import br.com.heaven.config.OpenApiConfig;
import br.com.heaven.dto.SorteioResposta;
import br.com.heaven.dto.VotoRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Validação")
@SecurityRequirement(name = OpenApiConfig.BEARER)
public class ValidacaoResource {

    private final ValidacaoBO validacoes;
    private final AcaoBO acoes;

    public ValidacaoResource(ValidacaoBO validacoes, AcaoBO acoes) {
        this.validacoes = validacoes;
        this.acoes = acoes;
    }

    @PostMapping("/sorteio")
    @Operation(summary = "Sorteia uma ação para validar",
            description = "Guarda a oferta por 10 minutos. 204 se não há ação elegível. O autor nunca aparece.")
    @RespostasDeErro({401, 403, 422})
    public ResponseEntity<SorteioResposta> sortear(@RequestAttribute(AuthInterceptor.USUARIO_ID) long usuarioId) throws SQLException {
        Optional<Sorteio> sorteio = validacoes.sortearAcao(usuarioId);
        if (sorteio.isEmpty()) return ResponseEntity.noContent().build();
        Sorteio s = sorteio.get();
        return ResponseEntity.ok(SorteioResposta.de(s.acao(), acoes.tiposPorId().get(s.acao().getIdTipoAcao()),
                s.expiraEm().atZone(ZoneId.systemDefault()).toOffsetDateTime()));
    }

    @PostMapping
    @Operation(summary = "Registra o voto",
            description = "Exige sorteio prévio da mesma ação (409 OFERTA_EXPIRADA). Autovalidação é 403, checada antes da oferta.")
    @RespostasDeErro(value = {400, 401, 403, 404, 409, 422}, sucesso = 201)
    public ResponseEntity<ResultadoVoto> votar(@RequestAttribute(AuthInterceptor.USUARIO_ID) long usuarioId,
                                               @Valid @RequestBody VotoRequest corpo) throws SQLException {
        ResultadoVoto r = validacoes.votar(usuarioId, corpo.idAcao(), corpo.voto(), corpo.justificativa());
        return ResponseEntity.status(HttpStatus.CREATED).body(r);
    }
}
