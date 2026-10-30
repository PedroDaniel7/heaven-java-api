package br.com.heaven.bo;

import br.com.heaven.dao.UsuarioDAO;
import br.com.heaven.model.NivelConfianca;
import br.com.heaven.model.Usuario;
import br.com.heaven.persistence.Jdbc;
import br.com.heaven.service.CodigoErro;
import br.com.heaven.service.RegraNegocioException;
import br.com.heaven.service.Regras;
import br.com.heaven.service.Senhas;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Cadastro, login, perfil e ranking de usuários. */
@Service
public class UsuarioBO extends BaseBO {

    private static final String PADRAO_EMAIL = "[^\\s@]+@[^\\s@]+\\.[^\\s@]+";
    private static final String CREDENCIAL_INVALIDA = "E-mail ou senha incorretos.";

    /** Hash descartável: login de e-mail inexistente gasta o mesmo tempo de um login real. */
    private static final class HashFalso {
        static final String VALOR = Senhas.criar("senha-inexistente".toCharArray());
    }

    public UsuarioBO(DataSource dataSource) {
        super(dataSource);
    }

    public PerfilUsuario cadastrar(String nome, String email, char[] senha) throws SQLException {
        String n = Regras.texto(nome, "Nome", 120);
        String e = Regras.texto(email, "E-mail", 150).toLowerCase(Locale.ROOT);
        if (!e.matches(PADRAO_EMAIL)) throw new RegraNegocioException(CodigoErro.VALIDACAO, "E-mail inválido.");
        String hash = Senhas.criar(senha);
        return transacao(c -> {
            Usuario u = new Usuario();
            u.setNome(n);
            u.setEmail(e);
            u.setSenhaHash(hash);
            u.setReputacaoAtual(new BigDecimal("50"));
            NivelConfianca nivel = nivel(c, u, true);
            u.setIdNivelConfianca(nivel.getId());
            new UsuarioDAO(c).inserir(u);
            return new PerfilUsuario(new UsuarioDAO(c).buscar(u.getId()).orElseThrow(), nivel.getNome());
        });
    }

    /** Confere a senha ANTES de revelar que a conta está inativa; credencial inválida tem mensagem única. */
    public PerfilUsuario autenticar(String email, char[] senha) throws SQLException {
        String e = Regras.texto(email, "E-mail", 150).toLowerCase(Locale.ROOT);
        return leitura(c -> {
            List<Long> ids = Jdbc.ids(c, "SELECT ID FROM USUARIO WHERE EMAIL=?", e);
            Usuario u = ids.isEmpty() ? null : new UsuarioDAO(c).buscar(ids.get(0)).orElse(null);
            boolean confere = Senhas.conferir(senha, u == null ? HashFalso.VALOR : u.getSenhaHash());
            if (u == null || !confere) throw new RegraNegocioException(CodigoErro.NAO_AUTENTICADO, CREDENCIAL_INVALIDA);
            if (!"ATIVO".equals(u.getStatusConta()))
                throw new RegraNegocioException(CodigoErro.PROIBIDO, "Conta não está ativa.");
            return new PerfilUsuario(u, nivel(c, u, false).getNome());
        });
    }

    public PerfilUsuario perfil(long id) throws SQLException {
        return leitura(c -> {
            Usuario u = ativo(c, id);
            return new PerfilUsuario(u, nivel(c, u, false).getNome());
        });
    }

    /** Atualiza nome e e-mail; a senha só muda quando {@code novaSenha} vem preenchida. E-mail repetido vira 409 pela restrição única. */
    public PerfilUsuario atualizar(long id, String nome, String email, char[] novaSenha) throws SQLException {
        String n = Regras.texto(nome, "Nome", 120);
        String e = Regras.texto(email, "E-mail", 150).toLowerCase(Locale.ROOT);
        if (!e.matches(PADRAO_EMAIL)) throw new RegraNegocioException(CodigoErro.VALIDACAO, "E-mail inválido.");
        String hash = novaSenha == null ? null : Senhas.criar(novaSenha);
        return transacao(c -> {
            Usuario u = ativo(c, id);
            u.setNome(n);
            u.setEmail(e);
            if (hash != null) u.setSenhaHash(hash);
            new UsuarioDAO(c).atualizar(u);
            return new PerfilUsuario(u, nivel(c, u, true).getNome());
        });
    }

    /** Desativação lógica da conta: STATUS_CONTA passa a INATIVO. */
    public void desativar(long id) throws SQLException {
        transacao(c -> {
            Usuario u = ativo(c, id);
            u.setStatusConta("INATIVO");
            new UsuarioDAO(c).atualizar(u);
            return null;
        });
    }

    public List<ItemRanking> ranking(int limite) throws SQLException {
        if (limite < 1 || limite > 100)
            throw new RegraNegocioException(CodigoErro.VALIDACAO, "O limite deve estar entre 1 e 100.");
        // limite já validado como inteiro; é inserido no texto porque FETCH FIRST não é parametrizado aqui.
        String sql = "SELECT POSICAO,USUARIO,PONTOS,NIVEL_CONFIANCA,STREAK FROM VW_RANKING_USUARIOS ORDER BY POSICAO,ID_USUARIO FETCH FIRST " + limite + " ROWS ONLY";
        return leitura(c -> {
            try (PreparedStatement s = c.prepareStatement(sql); ResultSet r = s.executeQuery()) {
                List<ItemRanking> lista = new ArrayList<>();
                while (r.next())
                    lista.add(new ItemRanking(r.getLong(1), r.getString(2), r.getLong(3), r.getString(4), r.getLong(5)));
                return lista;
            }
        });
    }
}
