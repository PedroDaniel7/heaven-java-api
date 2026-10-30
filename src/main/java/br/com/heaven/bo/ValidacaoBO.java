package br.com.heaven.bo;

import br.com.heaven.dao.AcaoUsuarioDAO;
import br.com.heaven.dao.HistoricoPontosDAO;
import br.com.heaven.dao.HistoricoReputacaoDAO;
import br.com.heaven.dao.IncidenteFraudeDAO;
import br.com.heaven.dao.PenalizacaoDAO;
import br.com.heaven.dao.UsuarioDAO;
import br.com.heaven.dao.ValidacaoDAO;
import br.com.heaven.model.AcaoUsuario;
import br.com.heaven.model.HistoricoPontos;
import br.com.heaven.model.HistoricoReputacao;
import br.com.heaven.model.IncidenteFraude;
import br.com.heaven.model.NivelConfianca;
import br.com.heaven.model.Penalizacao;
import br.com.heaven.model.Usuario;
import br.com.heaven.model.Validacao;
import br.com.heaven.persistence.Jdbc;
import br.com.heaven.service.CodigoErro;
import br.com.heaven.service.RegraNegocioException;
import br.com.heaven.service.Regras;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Sorteio e votação de ações, e penalização administrativa. As ofertas de sorteio ficam em
 * memória (10 minutos) e se perdem ao reiniciar o servidor; o voto sem oferta válida é recusado.
 */
@Service
public class ValidacaoBO extends BaseBO {

    private static final long VALIDADE_OFERTA_SEGUNDOS = 600;

    private record Oferta(long acao, Instant expira) {
    }

    private final Map<Long, Oferta> ofertas = new HashMap<>();
    private final SecureRandom random = new SecureRandom();

    private static final Logger LOG = LoggerFactory.getLogger(ValidacaoBO.class);

    /**
     * Modo de teste: cada sorteio troca a oferta anterior do usuário. Desligado por padrão e só ligado
     * com HEAVEN_SORTEIO_RESORTEAR=true; não deve ficar ligado em produção (permite garimpar ações).
     */
    private final boolean resortear;

    public ValidacaoBO(DataSource dataSource) {
        this(dataSource, "false");
    }

    @Autowired
    public ValidacaoBO(DataSource dataSource, @Value("${HEAVEN_SORTEIO_RESORTEAR:false}") String resortear) {
        super(dataSource);
        this.resortear = resortear != null && resortear.strip().equalsIgnoreCase("true");
        LOG.info("Modo de re-sorteio: {}", this.resortear ? "ativo" : "inativo");
    }

    /** Sorteia uma ação elegível de outro autor. Vazio quando não há nenhuma. */
    public synchronized Optional<Sorteio> sortearAcao(long validador) throws SQLException {
        return leitura(c -> {
            Usuario u = ativo(c, validador);
            bloqueio(c, validador, "BLOQUEIO_VALIDACAO");
            NivelConfianca n = nivel(c, u, false);
            Regras.verificarLimite(Jdbc.numero(c, "SELECT COUNT(*) FROM VALIDACAO WHERE ID_VALIDADOR=? AND DATA_HORA>=TRUNC(SYSDATE) AND DATA_HORA<TRUNC(SYSDATE)+1", validador), n.getLimiteValidacoesDia());
            List<Long> ids = Jdbc.ids(c, "SELECT A.ID FROM ACAO_USUARIO A JOIN USUARIO U ON U.ID=A.ID_USUARIO WHERE A.STATUS='PENDENTE' AND U.STATUS_CONTA='ATIVO' AND A.ID_USUARIO<>? AND NOT EXISTS (SELECT 1 FROM VALIDACAO V WHERE V.ID_ACAO=A.ID AND V.ID_VALIDADOR=?)", validador, validador);
            if (ids.isEmpty()) {
                ofertas.remove(validador);
                return Optional.<Sorteio>empty();
            }
            Oferta anterior = ofertas.get(validador);
            long id = !resortear && anterior != null && anterior.expira().isAfter(Instant.now()) && ids.contains(anterior.acao())
                    ? anterior.acao() : ids.get(random.nextInt(ids.size()));
            Oferta nova = new Oferta(id, Instant.now().plusSeconds(VALIDADE_OFERTA_SEGUNDOS));
            ofertas.put(validador, nova);
            AcaoUsuario acao = new AcaoUsuarioDAO(c).buscar(id).orElseThrow();
            return Optional.of(new Sorteio(acao, nova.expira()));
        });
    }

    /** Registra o voto; a justificativa é opcional. Os triggers do banco fecham a votação. */
    public synchronized ResultadoVoto votar(long validador, long acao, String voto, String justificativa) throws SQLException {
        if (!Set.of("APROVAR", "REJEITAR").contains(voto))
            throw new RegraNegocioException(CodigoErro.VALIDACAO, "Voto inválido.");
        String j = Regras.textoOpcional(justificativa, "Justificativa", 500);
        // Autovalidação (403) vem antes da oferta (409). Ação inexistente cai na checagem da oferta.
        if (leitura(c -> new AcaoUsuarioDAO(c).buscar(acao)).filter(a -> a.getIdUsuario() == validador).isPresent())
            throw new RegraNegocioException(CodigoErro.PROIBIDO, "Autovalidação proibida.");
        Oferta oferta = ofertas.get(validador);
        if (oferta == null || oferta.acao() != acao || oferta.expira().isBefore(Instant.now()))
            throw new RegraNegocioException(CodigoErro.OFERTA_EXPIRADA, "Solicite uma ação aleatória antes de votar; a oferta vale 10 minutos.");
        ResultadoVoto resultado = transacao(c -> {
            Usuario u = ativo(c, validador);
            bloqueio(c, validador, "BLOQUEIO_VALIDACAO");
            NivelConfianca n = nivel(c, u, true);
            AcaoUsuario a = new AcaoUsuarioDAO(c).buscar(acao)
                    .orElseThrow(() -> new RegraNegocioException(CodigoErro.NAO_ENCONTRADO, "Ação inexistente."));
            if (a.getIdUsuario() == validador)
                throw new RegraNegocioException(CodigoErro.PROIBIDO, "Autovalidação proibida.");
            if (!"PENDENTE".equals(a.getStatus()))
                throw new RegraNegocioException(CodigoErro.CONFLITO, "Ação já concluída.");
            if (!"ATIVO".equals(new UsuarioDAO(c).buscar(a.getIdUsuario()).orElseThrow().getStatusConta()))
                throw new RegraNegocioException(CodigoErro.REGRA, "A conta do autor não está ativa.");
            if (Jdbc.numero(c, "SELECT COUNT(*) FROM VALIDACAO WHERE ID_ACAO=? AND ID_VALIDADOR=?", acao, validador) > 0)
                throw new RegraNegocioException(CodigoErro.CONFLITO, "Voto duplicado.");
            Regras.verificarLimite(Jdbc.numero(c, "SELECT COUNT(*) FROM VALIDACAO WHERE ID_VALIDADOR=? AND DATA_HORA>=TRUNC(SYSDATE) AND DATA_HORA<TRUNC(SYSDATE)+1", validador), n.getLimiteValidacoesDia());
            Validacao v = new Validacao();
            v.setIdAcao(acao);
            v.setIdValidador(validador);
            v.setVoto(voto);
            v.setPesoVoto(n.getPesoVoto());
            v.setReputacaoValidadorMomento(u.getReputacaoAtual());
            v.setJustificativa(j);
            new ValidacaoDAO(c).inserir(v); // Triggers finalizam ação, pontos, streak e reputação.
            AcaoUsuario finalizada = new AcaoUsuarioDAO(c).buscar(acao).orElseThrow();
            boolean concluida = !"PENDENTE".equals(finalizada.getStatus());
            if (concluida)
                for (long id : Jdbc.ids(c, "SELECT ID_VALIDADOR FROM VALIDACAO WHERE ID_ACAO=?", acao))
                    nivel(c, new UsuarioDAO(c).buscar(id).orElseThrow(), true);
            return new ResultadoVoto(acao, voto, concluida);
        });
        ofertas.remove(validador);
        return resultado;
    }

    /** Uso administrativo: somente incidentes CONFIRMADOS e ainda não penalizados. */
    public void aplicarPenalizacao(long incidente, long pontos, BigDecimal reducao) throws SQLException {
        if (pontos < 0 || pontos > 99999999L || reducao == null || reducao.signum() < 0 || reducao.compareTo(new BigDecimal("100")) > 0 || reducao.stripTrailingZeros().scale() > 2 || pontos == 0 && reducao.signum() == 0)
            throw new RegraNegocioException(CodigoErro.VALIDACAO, "Penalização inválida.");
        transacao(c -> {
            IncidenteFraude i = new IncidenteFraudeDAO(c).buscar(incidente)
                    .orElseThrow(() -> new RegraNegocioException(CodigoErro.NAO_ENCONTRADO, "Incidente inexistente."));
            if (!"CONFIRMADO".equals(i.getStatus()))
                throw new RegraNegocioException(CodigoErro.REGRA, "Confirme o incidente antes de penalizar.");
            if (Jdbc.numero(c, "SELECT COUNT(*) FROM PENALIZACAO WHERE ID_INCIDENTE=?", incidente) > 0)
                throw new RegraNegocioException(CodigoErro.CONFLITO, "Incidente já penalizado.");
            Usuario u = new UsuarioDAO(c).buscar(i.getIdUsuario()).orElseThrow();
            long perda = Math.min(pontos, u.getPontosAtuais());
            BigDecimal delta = reducao.min(u.getReputacaoAtual());
            Penalizacao p = new Penalizacao();
            p.setIdUsuario(u.getId());
            p.setIdIncidente(incidente);
            p.setTipoPenalizacao(pontos > 0 ? "PERDA_PONTOS" : "REDUCAO_REPUTACAO");
            p.setPontosPerdidos(perda);
            p.setReputacaoReduzida(delta);
            p.setDescricao("Incidente confirmado #" + incidente);
            p.setStatus("ENCERRADA");
            new PenalizacaoDAO(c).inserir(p);
            if (perda > 0) {
                HistoricoPontos h = new HistoricoPontos();
                h.setIdUsuario(u.getId());
                h.setQuantidade(-perda);
                h.setTipoMovimentacao("PENALIZACAO");
                h.setDescricao(p.getDescricao());
                new HistoricoPontosDAO(c).inserir(h);
            }
            if (delta.signum() > 0) {
                HistoricoReputacao h = new HistoricoReputacao();
                h.setIdUsuario(u.getId());
                h.setReputacaoAnterior(u.getReputacaoAtual());
                h.setReputacaoNova(u.getReputacaoAtual().subtract(delta));
                h.setMotivo(p.getDescricao());
                new HistoricoReputacaoDAO(c).inserir(h);
            }
            nivel(c, new UsuarioDAO(c).buscar(u.getId()).orElseThrow(), true);
            return null;
        });
    }
}
