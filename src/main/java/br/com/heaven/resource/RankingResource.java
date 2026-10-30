package br.com.heaven.resource;

import br.com.heaven.bo.ItemRanking;
import br.com.heaven.bo.UsuarioBO;
import br.com.heaven.config.Publico;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.sql.SQLException;
import java.util.List;

@RestController
@RequestMapping("/api/ranking")
public class RankingResource {

    private final UsuarioBO usuarios;

    public RankingResource(UsuarioBO usuarios) {
        this.usuarios = usuarios;
    }

    /** O limite de 1 a 100 é validado no BO (fora da faixa vira 400 VALIDACAO). */
    @Publico
    @GetMapping
    public List<ItemRanking> ranking(@RequestParam(defaultValue = "20") int limite) throws SQLException {
        return usuarios.ranking(limite);
    }
}
