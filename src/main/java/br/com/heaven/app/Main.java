package br.com.heaven.app;
import br.com.heaven.config.ConexaoOracle;
import br.com.heaven.model.*;
import br.com.heaven.service.*;
import java.math.BigDecimal;
import java.util.*;
public final class Main {
    private static final Scanner ENTRADA=new Scanner(System.in);
    private static String ler(String pergunta) {System.out.print(pergunta+": ");return ENTRADA.nextLine().strip();}
    private static char[] senha() {
        if(System.console()!=null) return System.console().readPassword("Senha: ");
        return ler("Senha (visível no console do IntelliJ)").toCharArray();
    }
    public static void main(String[] args) {
        HeavenService service=new HeavenService(ConexaoOracle::abrir);Usuario usuario=null;
        System.out.println("HEAVEN | Ações sustentáveis, impacto compartilhado");
        while(true) {
            try {
                if(usuario==null) {
                    System.out.println("1 Cadastrar | 2 Entrar | 0 Sair");String op=ler("Opção");
                    if(op.equals("0")) return;
                    if(op.equals("1")) {String nome=ler("Nome"),email=ler("E-mail");char[] s=senha();try{usuario=service.cadastrar(nome,email,s);}finally{Arrays.fill(s,'\0');}}
                    else if(op.equals("2")) {String email=ler("E-mail");char[] s=senha();try{usuario=service.autenticar(email,s);}finally{Arrays.fill(s,'\0');}}
                    else System.out.println("Opção inválida.");
                } else {
                    System.out.println("1 Perfil | 2 Registrar ação | 3 Minhas ações | 4 Validar ação sorteada | 5 Ranking | 6 Cancelar ação | 9 Sair da conta | 0 Encerrar");
                    String op=ler("Opção");long id=usuario.getId();
                    switch(op) {
                        case "0": return;
                        case "9": usuario=null;break;
                        case "1": {Usuario u=service.perfil(id);System.out.printf("%s | %d pontos | reputação %s | streak %d | recorde %d%n",u.getNome(),u.getPontosAtuais(),u.getReputacaoAtual(),u.getStreakAtual(),u.getMaiorStreak());break;}
                        case "2": {
                            for(TipoAcao t:service.tiposAtivos()) System.out.println(t.getId()+" - "+t.getNome()+" ("+t.getUnidadeMedida()+")");
                            AcaoUsuario a=service.registrarAcao(id,Long.parseLong(ler("ID do tipo")),new BigDecimal(ler("Quantidade").replace(',','.')),ler("Descreva a evidência"));
                            System.out.println("Ação #"+a.getId()+" PENDENTE | pontos previstos: "+a.getPontosPrevistos());break;
                        }
                        case "3": for(AcaoUsuario a:service.minhasAcoes(id)) System.out.println("#"+a.getId()+" | "+a.getStatus()+" | "+a.getPontosPrevistos()+" pontos | "+a.getDescricaoEvidencia());break;
                        case "4": {
                            Optional<AcaoUsuario> oferta=service.sortearAcao(id);
                            if(oferta.isEmpty()) {System.out.println("Nenhuma ação elegível.");break;}
                            AcaoUsuario a=oferta.get();
                            String tipoNome=service.tiposAtivos().stream().filter(t->t.getId().equals(a.getIdTipoAcao())).map(t->t.getNome()+" / "+t.getUnidadeMedida()).findFirst().orElse("Tipo #"+a.getIdTipoAcao());
                            System.out.println("#"+a.getId()+" | "+tipoNome+" | quantidade "+a.getQuantidade()+" | "+a.getDataHora()+" | "+a.getDescricaoEvidencia());
                            String v=ler("APROVAR, REJEITAR ou VOLTAR").toUpperCase(Locale.ROOT);if(v.equals("VOLTAR"))break;
                            System.out.println("Resultado: "+service.votar(id,a.getId(),v,ler("Justificativa")).getStatus());break;
                        }
                        case "5": service.ranking().forEach(System.out::println);break;
                        case "6": service.cancelarAcao(id,Long.parseLong(ler("ID da ação")));System.out.println("Ação cancelada.");break;
                        default: System.out.println("Opção inválida.");
                    }
                }
            } catch(NoSuchElementException e) {return;}
              catch(Exception e) {System.out.println("Não foi possível concluir: "+e.getMessage());}
        }
    }
}
