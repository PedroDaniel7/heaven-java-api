package br.com.heaven.test;
import br.com.heaven.config.ConexaoOracle;
import br.com.heaven.dao.*;
import br.com.heaven.model.*;
import br.com.heaven.persistence.Jdbc;
import br.com.heaven.service.*;
import java.lang.reflect.*;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;
/** Integração real, somente schema de TESTES vazio de usuários. Tudo termina em rollback. */
public final class TesteOracle {
    private static int testes;
    private static void ok(boolean b,String texto){if(!b)throw new AssertionError(texto);testes++;System.out.println("OK "+texto);}
    @FunctionalInterface private interface Acao {void run() throws Exception;}
    private static void rejeita(Acao a,String texto)throws Exception{
        try{a.run();throw new AssertionError("Não rejeitou: "+texto);}catch(RegraNegocioException e){ok(true,texto);}
    }
    // Cada caso de uso tem um savepoint: commit/close não encerram a transação externa de teste.
    private static Connection emprestar(Connection c)throws SQLException {
        Savepoint save=c.setSavepoint();
        return (Connection)Proxy.newProxyInstance(Connection.class.getClassLoader(),new Class<?>[]{Connection.class},(p,m,a)->{
            switch(m.getName()){
                case "close":case "commit":case "setAutoCommit":return null;
                case "rollback":c.rollback(save);return null;
                default:try{return m.invoke(c,a);}catch(InvocationTargetException e){throw e.getCause();}
            }
        });
    }
    private static <T> void crud(CrudDAO<T> dao,T item,java.util.function.Function<T,Long> id,java.util.function.Consumer<T> editar,java.util.function.Predicate<T> atualizado,String nome)throws Exception {
        dao.inserir(item);Long chave=id.apply(item);ok(chave!=null,nome+" CREATE / identity");
        T lido=dao.buscar(chave).orElseThrow();ok(dao.listar().stream().anyMatch(x->chave.equals(id.apply(x))),nome+" READ / listagem");
        editar.accept(lido);dao.atualizar(lido);ok(atualizado.test(dao.buscar(chave).orElseThrow()),nome+" UPDATE persistido");
        ok(dao.excluir(chave)&&dao.buscar(chave).isEmpty(),nome+" DELETE");
        ok(!dao.excluir(chave),nome+" DELETE inexistente");
    }
    public static void main(String[] args)throws Exception {
        if(args.length!=1 || !args[0].equals("--schema-teste"))throw new IllegalArgumentException("Use --schema-teste e credenciais de schema de testes com Heaven e carga inicial, sem usuários.");
        try(Connection c=ConexaoOracle.abrir()){
            c.setAutoCommit(false);
            try{
                ok(Jdbc.numero(c,"SELECT COUNT(*) FROM USUARIO")==0,"schema sem usuários (pré-condição)");
                Jdbc.executar(c,"UPDATE PARAMETRO_SISTEMA SET VALOR=3 WHERE NOME_PARAMETRO='MIN_VALIDACOES_POR_ACAO'");
                Jdbc.executar(c,"UPDATE PARAMETRO_SISTEMA SET VALOR=60 WHERE NOME_PARAMETRO='PERCENTUAL_APROVACAO_MINIMO'");
                NivelConfianca n=new NivelConfianca();n.setNome("CRUD nivel");n.setReputacaoMinima(BigDecimal.ZERO);n.setPesoVoto(BigDecimal.ONE);n.setLimiteAcoesDia(5L);n.setLimiteValidacoesDia(10L);
                crud(new NivelConfiancaDAO(c),n,NivelConfianca::getId,x->x.setNome("CRUD nivel editado"),x->x.getNome().equals("CRUD nivel editado"),"NivelConfianca");
                ParametroSistema p=new ParametroSistema();p.setNomeParametro("CRUD_PARAM");p.setValor(BigDecimal.ONE);
                crud(new ParametroSistemaDAO(c),p,ParametroSistema::getId,x->x.setValor(BigDecimal.TEN),x->x.getValor().compareTo(BigDecimal.TEN)==0,"ParametroSistema");
                Conquista co=new Conquista();co.setNome("CRUD conquista");co.setRequisito("Teste");
                crud(new ConquistaDAO(c),co,Conquista::getId,x->x.setNome("CRUD conquista editada"),x->x.getNome().equals("CRUD conquista editada"),"Conquista");
                TipoAcao t=new TipoAcao();t.setNome("CRUD tipo");t.setCategoria("OUTRO");t.setUnidadeMedida("un");t.setImpactoAmbientalEstimado(BigDecimal.ONE);t.setPontosBase(10L);t.setDificuldade("FACIL");
                crud(new TipoAcaoDAO(c),t,TipoAcao::getId,x->x.setNome("CRUD tipo editado"),x->x.getNome().equals("CRUD tipo editado"),"TipoAcao");
                long nivel=Jdbc.numero(c,"SELECT MIN(ID) FROM NIVEL_CONFIANCA");
                Usuario cru=new Usuario();cru.setNome("CRUD usuário");cru.setEmail("crud@heaven.test");cru.setSenhaHash(Senhas.criar("Teste12345".toCharArray()));cru.setIdNivelConfianca(nivel);
                crud(new UsuarioDAO(c),cru,Usuario::getId,x->x.setNome("CRUD editado"),x->x.getNome().equals("CRUD editado"),"Usuario");
                HeavenService s=new HeavenService(()->emprestar(c));
                Usuario autor=s.cadastrar("Autor","autor@heaven.test","Teste12345".toCharArray());
                Usuario v1=s.cadastrar("Validador 1","v1@heaven.test","Teste12345".toCharArray());
                Usuario v2=s.cadastrar("Validador 2","v2@heaven.test","Teste12345".toCharArray());
                Usuario v3=s.cadastrar("Validador 3","v3@heaven.test","Teste12345".toCharArray());
                ok(s.autenticar("autor@heaven.test","Teste12345".toCharArray()).getId().equals(autor.getId()),"login correto");
                rejeita(()->s.autenticar("autor@heaven.test","SenhaErrada".toCharArray()),"login incorreto");
                IncidenteFraude inc=new IncidenteFraude();inc.setIdUsuario(autor.getId());inc.setTipoIncidente("COMPORTAMENTO_ANORMAL");inc.setNivelSeveridade("BAIXO");
                crud(new IncidenteFraudeDAO(c),inc,IncidenteFraude::getId,x->x.setStatus("DESCARTADO"),x->x.getStatus().equals("DESCARTADO"),"IncidenteFraude");
                long tipo=s.tiposAtivos().get(0).getId();
                AcaoUsuario a=s.registrarAcao(autor.getId(),tipo,BigDecimal.TEN,"Evidência de teste");ok("PENDENTE".equals(a.getStatus()),"registro pendente");
                ok(s.sortearAcao(autor.getId()).isEmpty(),"sorteio exclui própria ação");
                rejeita(()->s.votar(autor.getId(),a.getId(),"APROVAR","Teste"),"autovoto sem oferta");
                for(Usuario v:List.of(v1,v2,v3)){
                    ok(s.sortearAcao(v.getId()).orElseThrow().getId().equals(a.getId()),"oferta elegível");
                    s.votar(v.getId(),a.getId(),"APROVAR","Evidência conferida");
                }
                ok("APROVADA".equals(new AcaoUsuarioDAO(c).buscar(a.getId()).orElseThrow().getStatus()),"trigger aprova no quórum");
                Usuario saldo=s.perfil(autor.getId());ok(saldo.getPontosAtuais().equals(a.getPontosPrevistos()),"pontos creditados uma vez");ok(saldo.getStreakAtual()==1,"streak inicial por trigger");
                ok(s.perfil(v1.getId()).getReputacaoAtual().compareTo(new BigDecimal("51"))==0,"reputação por convergência");
                rejeita(()->s.votar(v1.getId(),a.getId(),"APROVAR","Repetição"),"repetição sem oferta");
                AcaoUsuario b=s.registrarAcao(autor.getId(),tipo,BigDecimal.TEN,"Segunda evidência");
                for(Usuario v:List.of(v1,v2,v3)){s.sortearAcao(v.getId());s.votar(v.getId(),b.getId(),"REJEITAR","Evidência insuficiente");}
                ok("REJEITADA".equals(new AcaoUsuarioDAO(c).buscar(b.getId()).orElseThrow().getStatus()),"trigger rejeita no quórum");
                ok(s.perfil(autor.getId()).getPontosAtuais().equals(a.getPontosPrevistos()),"rejeição sem pontos");
                AcaoUsuario cancelada=s.registrarAcao(autor.getId(),tipo,BigDecimal.ONE,"Cancelar teste");s.cancelarAcao(autor.getId(),cancelada.getId());
                ok("CANCELADA".equals(new AcaoUsuarioDAO(c).buscar(cancelada.getId()).orElseThrow().getStatus()),"cancelamento sem votos");
                rejeita(()->s.cancelarAcao(autor.getId(),a.getId()),"não cancela ação concluída");
                long nivelAtual=s.perfil(autor.getId()).getIdNivelConfianca();
                Jdbc.executar(c,"UPDATE NIVEL_CONFIANCA SET LIMITE_ACOES_DIA=3 WHERE ID=?",nivelAtual);
                rejeita(()->s.registrarAcao(autor.getId(),tipo,BigDecimal.ONE,"Limite"),"limite de ações inclui canceladas");
                IncidenteFraude confirmado=new IncidenteFraude();confirmado.setIdUsuario(autor.getId());confirmado.setTipoIncidente("COMPORTAMENTO_ANORMAL");confirmado.setNivelSeveridade("MEDIO");confirmado.setStatus("CONFIRMADO");new IncidenteFraudeDAO(c).inserir(confirmado);
                s.aplicarPenalizacao(confirmado.getId(),999L,new BigDecimal("100"));
                ok(s.perfil(autor.getId()).getPontosAtuais()==0,"penalização respeita saldo");ok(s.perfil(autor.getId()).getReputacaoAtual().signum()==0,"penalização respeita piso reputação");
                rejeita(()->s.aplicarPenalizacao(confirmado.getId(),1L,BigDecimal.ONE),"penalização idempotente por incidente");
                ok(!s.ranking().isEmpty(),"view de ranking");
                System.out.println("TOTAL ORACLE: "+testes+" verificações. Dados serão desfeitos.");
            }finally{c.rollback();System.out.println("ROLLBACK concluído (identities podem deixar lacunas).");}
        }
    }
}
