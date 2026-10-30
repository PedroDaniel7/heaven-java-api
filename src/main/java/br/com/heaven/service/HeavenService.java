package br.com.heaven.service;
import br.com.heaven.dao.*;
import br.com.heaven.model.*;
import br.com.heaven.persistence.Jdbc;
import java.math.*;
import java.security.SecureRandom;
import java.sql.*;
import java.time.*;
import java.util.*;
/** Uma instância por aplicação de console. Cada caso de uso abre sua própria transação. */
public class HeavenService {
    @FunctionalInterface public interface FabricaConexao { Connection abrir() throws SQLException; }
    @FunctionalInterface private interface Operacao<T> { T executar(Connection c) throws SQLException; }
    private final FabricaConexao fabrica;
    private final Map<Long,Oferta> ofertas=new HashMap<>();
    private final SecureRandom random=new SecureRandom();
    private record Oferta(long acao,Instant expira) {}
    public HeavenService(FabricaConexao fabrica) { this.fabrica=Objects.requireNonNull(fabrica); }
    private <T> T transacao(Operacao<T> op) throws SQLException {
        try(Connection c=fabrica.abrir()) {
            c.setAutoCommit(false);
            try {
                // Mutex no banco: serializa os casos de uso Java, inclusive entre processos.
                // Evita corridas com as triggers existentes sem alterar o DDL entregue.
                Jdbc.numero(c,"SELECT ID FROM PARAMETRO_SISTEMA WHERE NOME_PARAMETRO='MIN_VALIDACOES_POR_ACAO' FOR UPDATE");
                T resultado=op.executar(c); c.commit(); return resultado;
            } catch(SQLException|RuntimeException e) {
                try { c.rollback(); } catch(SQLException rollback) { e.addSuppressed(rollback); }
                throw e;
            }
        }
    }
    private Usuario ativo(Connection c,long id) throws SQLException {
        Usuario u=new UsuarioDAO(c).buscar(id).orElseThrow(()->new RegraNegocioException("Usuário não encontrado."));
        if(!"ATIVO".equals(u.getStatusConta())) throw new RegraNegocioException("Conta não está ativa.");
        return u;
    }
    private NivelConfianca nivel(Connection c,Usuario u) throws SQLException {
        List<NivelConfianca> niveis=new NivelConfiancaDAO(c).listar();
        NivelConfianca n=niveis.stream().filter(x->x.getReputacaoMinima().compareTo(u.getReputacaoAtual())<=0)
            .max(Comparator.comparing(NivelConfianca::getReputacaoMinima).thenComparing(NivelConfianca::getId))
            .orElseThrow(()->new RegraNegocioException("Cadastre um nível com reputação mínima zero."));
        if(u.getId()!=null && !n.getId().equals(u.getIdNivelConfianca())) Jdbc.executar(c,"UPDATE USUARIO SET ID_NIVEL_CONFIANCA=? WHERE ID=?",n.getId(),u.getId());
        return n;
    }
    private void bloqueio(Connection c,long id,String tipo) throws SQLException {
        if(Jdbc.numero(c,"SELECT COUNT(*) FROM PENALIZACAO WHERE ID_USUARIO=? AND STATUS='ATIVA' AND DATA_INICIO<=SYSDATE AND (DATA_FIM IS NULL OR DATA_FIM>SYSDATE) AND TIPO_PENALIZACAO IN (?, 'SUSPENSAO_CONTA')",id,tipo)>0)
            throw new RegraNegocioException("Operação bloqueada por penalização ativa.");
    }
    public Usuario cadastrar(String nome,String email,char[] senha) throws SQLException {
        String n=Regras.texto(nome,"Nome",120),e=Regras.texto(email,"E-mail",150).toLowerCase(Locale.ROOT);
        if(!e.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) throw new RegraNegocioException("E-mail inválido.");
        String hash=Senhas.criar(senha);
        return transacao(c->{
            Usuario u=new Usuario(); u.setNome(n);u.setEmail(e);u.setSenhaHash(hash);u.setReputacaoAtual(new BigDecimal("50"));
            NivelConfianca nivel=nivel(c,u);u.setIdNivelConfianca(nivel.getId());
            new UsuarioDAO(c).inserir(u); return new UsuarioDAO(c).buscar(u.getId()).orElseThrow();
        });
    }
    public Usuario autenticar(String email,char[] senha) throws SQLException {
        return transacao(c->{
            List<Long> ids=Jdbc.ids(c,"SELECT ID FROM USUARIO WHERE EMAIL=?",Regras.texto(email,"E-mail",150).toLowerCase(Locale.ROOT));
            if(ids.isEmpty()) throw new RegraNegocioException("E-mail ou senha incorretos.");
            Usuario u=ativo(c,ids.get(0));
            if(!Senhas.conferir(senha,u.getSenhaHash())) throw new RegraNegocioException("E-mail ou senha incorretos.");
            nivel(c,u); return new UsuarioDAO(c).buscar(u.getId()).orElseThrow();
        });
    }
    public Usuario perfil(long id) throws SQLException {
        return transacao(c->{Usuario u=ativo(c,id);nivel(c,u);return new UsuarioDAO(c).buscar(id).orElseThrow();});
    }
    public List<TipoAcao> tiposAtivos() throws SQLException {
        try(Connection c=fabrica.abrir()) {return new TipoAcaoDAO(c).listar().stream().filter(t->"S".equals(t.getStatusAtivo())).toList();}
    }
    public AcaoUsuario registrarAcao(long usuario,long tipo,BigDecimal quantidade,String evidencia) throws SQLException {
        String ev=Regras.texto(evidencia,"Evidência",500);
        return transacao(c->{
            Usuario u=ativo(c,usuario);bloqueio(c,usuario,"BLOQUEIO_ACAO");NivelConfianca n=nivel(c,u);
            Regras.verificarLimite(Jdbc.numero(c,"SELECT COUNT(*) FROM ACAO_USUARIO WHERE ID_USUARIO=? AND DATA_HORA>=TRUNC(SYSDATE) AND DATA_HORA<TRUNC(SYSDATE)+1",usuario),n.getLimiteAcoesDia());
            TipoAcao t=new TipoAcaoDAO(c).buscar(tipo).orElseThrow(()->new RegraNegocioException("Tipo não encontrado."));
            if(!"S".equals(t.getStatusAtivo())) throw new RegraNegocioException("Tipo de ação inativo.");
            AcaoUsuario a=new AcaoUsuario();a.setIdUsuario(usuario);a.setIdTipoAcao(tipo);a.setQuantidade(quantidade);
            a.setImpactoAmbientalCalculado(Regras.calcularImpacto(quantidade,t.getImpactoAmbientalEstimado()));
            a.setPontosPrevistos(Regras.calcularPontos(a.getImpactoAmbientalCalculado(),t.getPontosBase()));
            a.setDescricaoEvidencia(ev);new AcaoUsuarioDAO(c).inserir(a);
            return new AcaoUsuarioDAO(c).buscar(a.getId()).orElseThrow();
        });
    }
    public List<AcaoUsuario> minhasAcoes(long usuario) throws SQLException {
        try(Connection c=fabrica.abrir()) {
            ativo(c,usuario); List<AcaoUsuario> lista=new ArrayList<>();AcaoUsuarioDAO dao=new AcaoUsuarioDAO(c);
            for(long id:Jdbc.ids(c,"SELECT ID FROM ACAO_USUARIO WHERE ID_USUARIO=? ORDER BY DATA_HORA DESC",usuario)) lista.add(dao.buscar(id).orElseThrow());
            return lista;
        }
    }
    public void cancelarAcao(long usuario,long acao) throws SQLException {
        transacao(c->{
            ativo(c,usuario);
            if(Jdbc.numero(c,"SELECT COUNT(*) FROM VALIDACAO WHERE ID_ACAO=?",acao)>0) throw new RegraNegocioException("Uma ação com votos não pode ser cancelada.");
            if(Jdbc.executar(c,"UPDATE ACAO_USUARIO SET STATUS='CANCELADA',DATA_CONCLUSAO=SYSDATE WHERE ID=? AND ID_USUARIO=? AND STATUS='PENDENTE'",acao,usuario)!=1)
                throw new RegraNegocioException("Ação não encontrada, de outro usuário ou já concluída.");
            return null;
        });
    }
    public synchronized Optional<AcaoUsuario> sortearAcao(long validador) throws SQLException {
        return transacao(c->{
            Usuario u=ativo(c,validador);bloqueio(c,validador,"BLOQUEIO_VALIDACAO");NivelConfianca n=nivel(c,u);
            Regras.verificarLimite(Jdbc.numero(c,"SELECT COUNT(*) FROM VALIDACAO WHERE ID_VALIDADOR=? AND DATA_HORA>=TRUNC(SYSDATE) AND DATA_HORA<TRUNC(SYSDATE)+1",validador),n.getLimiteValidacoesDia());
            List<Long> ids=Jdbc.ids(c,"SELECT A.ID FROM ACAO_USUARIO A JOIN USUARIO U ON U.ID=A.ID_USUARIO WHERE A.STATUS='PENDENTE' AND U.STATUS_CONTA='ATIVO' AND A.ID_USUARIO<>? AND NOT EXISTS (SELECT 1 FROM VALIDACAO V WHERE V.ID_ACAO=A.ID AND V.ID_VALIDADOR=?)",validador,validador);
            if(ids.isEmpty()) {ofertas.remove(validador);return Optional.empty();}
            Oferta anterior=ofertas.get(validador);
            long id=anterior!=null && anterior.expira().isAfter(Instant.now()) && ids.contains(anterior.acao())?anterior.acao():ids.get(random.nextInt(ids.size()));
            ofertas.put(validador,new Oferta(id,Instant.now().plusSeconds(600)));
            return new AcaoUsuarioDAO(c).buscar(id);
        });
    }
    public synchronized AcaoUsuario votar(long validador,long acao,String voto,String justificativa) throws SQLException {
        if(!Set.of("APROVAR","REJEITAR").contains(voto)) throw new RegraNegocioException("Voto inválido.");
        String j=Regras.texto(justificativa,"Justificativa",500);
        Oferta oferta=ofertas.get(validador);
        if(oferta==null || oferta.acao()!=acao || oferta.expira().isBefore(Instant.now())) throw new RegraNegocioException("Solicite uma ação aleatória antes de votar; oferta vale 10 minutos.");
        AcaoUsuario resultado=transacao(c->{
            Usuario u=ativo(c,validador);bloqueio(c,validador,"BLOQUEIO_VALIDACAO");NivelConfianca n=nivel(c,u);
            AcaoUsuario a=new AcaoUsuarioDAO(c).buscar(acao).orElseThrow(()->new RegraNegocioException("Ação inexistente."));
            if(a.getIdUsuario()==validador) throw new RegraNegocioException("Autovalidação proibida.");
            if(!"PENDENTE".equals(a.getStatus())) throw new RegraNegocioException("Ação já concluída.");
            ativo(c,a.getIdUsuario());
            if(Jdbc.numero(c,"SELECT COUNT(*) FROM VALIDACAO WHERE ID_ACAO=? AND ID_VALIDADOR=?",acao,validador)>0) throw new RegraNegocioException("Voto duplicado.");
            Regras.verificarLimite(Jdbc.numero(c,"SELECT COUNT(*) FROM VALIDACAO WHERE ID_VALIDADOR=? AND DATA_HORA>=TRUNC(SYSDATE) AND DATA_HORA<TRUNC(SYSDATE)+1",validador),n.getLimiteValidacoesDia());
            Validacao v=new Validacao();v.setIdAcao(acao);v.setIdValidador(validador);v.setVoto(voto);v.setPesoVoto(n.getPesoVoto());v.setReputacaoValidadorMomento(u.getReputacaoAtual());v.setJustificativa(j);
            new ValidacaoDAO(c).inserir(v); // Triggers finalizam ação, pontos, streak e reputação.
            AcaoUsuario finalizada=new AcaoUsuarioDAO(c).buscar(acao).orElseThrow();
            if(!"PENDENTE".equals(finalizada.getStatus()))
                for(long id:Jdbc.ids(c,"SELECT ID_VALIDADOR FROM VALIDACAO WHERE ID_ACAO=?",acao)) nivel(c,new UsuarioDAO(c).buscar(id).orElseThrow());
            return finalizada;
        });
        ofertas.remove(validador);return resultado;
    }
    /** Uso administrativo local: somente incidentes CONFIRMADOS e ainda não penalizados. */
    public void aplicarPenalizacao(long incidente,long pontos,BigDecimal reducao) throws SQLException {
        if(pontos<0 || pontos>99999999L || reducao==null || reducao.signum()<0 || reducao.compareTo(new BigDecimal("100"))>0 || reducao.stripTrailingZeros().scale()>2 || pontos==0 && reducao.signum()==0)
            throw new RegraNegocioException("Penalização inválida.");
        transacao(c->{
            IncidenteFraude i=new IncidenteFraudeDAO(c).buscar(incidente).orElseThrow(()->new RegraNegocioException("Incidente inexistente."));
            if(!"CONFIRMADO".equals(i.getStatus())) throw new RegraNegocioException("Confirme o incidente antes de penalizar.");
            if(Jdbc.numero(c,"SELECT COUNT(*) FROM PENALIZACAO WHERE ID_INCIDENTE=?",incidente)>0) throw new RegraNegocioException("Incidente já penalizado.");
            Usuario u=new UsuarioDAO(c).buscar(i.getIdUsuario()).orElseThrow();
            long perda=Math.min(pontos,u.getPontosAtuais());BigDecimal delta=reducao.min(u.getReputacaoAtual());
            Penalizacao p=new Penalizacao();p.setIdUsuario(u.getId());p.setIdIncidente(incidente);p.setTipoPenalizacao(pontos>0?"PERDA_PONTOS":"REDUCAO_REPUTACAO");p.setPontosPerdidos(perda);p.setReputacaoReduzida(delta);p.setDescricao("Incidente confirmado #"+incidente);p.setStatus("ENCERRADA");new PenalizacaoDAO(c).inserir(p);
            if(perda>0) {HistoricoPontos h=new HistoricoPontos();h.setIdUsuario(u.getId());h.setQuantidade(-perda);h.setTipoMovimentacao("PENALIZACAO");h.setDescricao(p.getDescricao());new HistoricoPontosDAO(c).inserir(h);}
            if(delta.signum()>0) {HistoricoReputacao h=new HistoricoReputacao();h.setIdUsuario(u.getId());h.setReputacaoAnterior(u.getReputacaoAtual());h.setReputacaoNova(u.getReputacaoAtual().subtract(delta));h.setMotivo(p.getDescricao());new HistoricoReputacaoDAO(c).inserir(h);}
            nivel(c,new UsuarioDAO(c).buscar(u.getId()).orElseThrow());return null;
        });
    }
    public List<String> ranking() throws SQLException {
        try(Connection c=fabrica.abrir();PreparedStatement s=c.prepareStatement("SELECT POSICAO,USUARIO,PONTOS,STREAK FROM VW_RANKING_USUARIOS ORDER BY POSICAO,ID_USUARIO FETCH FIRST 20 ROWS ONLY");ResultSet r=s.executeQuery()) {
            List<String> lista=new ArrayList<>();while(r.next()) lista.add(r.getLong(1)+" | "+r.getString(2)+" | "+r.getLong(3)+" pontos | streak "+r.getLong(4));return lista;
        }
    }
}
