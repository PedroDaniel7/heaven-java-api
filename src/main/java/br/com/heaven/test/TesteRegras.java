package br.com.heaven.test;
import br.com.heaven.service.*;
import br.com.heaven.model.*;
import br.com.heaven.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
public final class TesteRegras {
    private static int testes;
    private static BigDecimal d(String v){return new BigDecimal(v);}
    private static void igual(Object esperado,Object atual,String caso) {
        if(!Objects.equals(esperado,atual))throw new AssertionError(caso+": esperado "+esperado+", obtido "+atual);
        testes++;System.out.println("OK "+caso);
    }
    private static void rejeita(Runnable acao,String caso) {
        try{acao.run();throw new AssertionError("Não rejeitou: "+caso);}catch(RegraNegocioException e){testes++;System.out.println("OK "+caso);}
    }
    public static void main(String[] args) {
        igual(d("1.00"),Regras.calcularImpacto(d("10"),d("0.10")),"impacto por quantidade");
        igual(d("0.01"),Regras.calcularImpacto(d("0.05"),d("0.1")),"arredondamento do impacto");
        rejeita(()->Regras.calcularImpacto(d("0"),d("1")),"quantidade zero");
        rejeita(()->Regras.calcularImpacto(d("-1"),d("1")),"quantidade negativa");
        rejeita(()->Regras.calcularImpacto(d("1.001"),d("1")),"precisão da quantidade");
        rejeita(()->Regras.calcularImpacto(d("100000000"),d("1")),"overflow quantidade");
        rejeita(()->Regras.calcularImpacto(d("99999999"),d("99999999")),"overflow impacto");
        igual(10L,Regras.calcularPontos(d("1"),10),"pontos por impacto");
        igual(1L,Regras.calcularPontos(d("0.01"),1),"mínimo compatível com trigger");
        igual(3L,Regras.calcularPontos(d("0.25"),10),"arredondamento de pontos");
        rejeita(()->Regras.calcularPontos(d("100000000"),10),"overflow pontos");
        rejeita(()->Regras.calcularPontos(d("0"),10),"impacto arredondado a zero");
        igual("PENDENTE",Regras.decidirResultado(2,3,d("2"),d("2"),d("60")),"quórum incompleto");
        igual("APROVADA",Regras.decidirResultado(3,3,d("3"),d("5"),d("60")),"limiar exato 60%");
        igual("REJEITADA",Regras.decidirResultado(3,3,d("2.99"),d("5"),d("60")),"abaixo do limiar");
        igual("APROVADA",Regras.decidirResultado(3,3,d("3"),d("4"),d("60")),"maioria por peso");
        rejeita(()->Regras.decidirResultado(3,0,d("1"),d("3"),d("60")),"quórum inválido");
        rejeita(()->Regras.decidirResultado(3,3,d("0"),d("0"),d("60")),"peso total zero");
        LocalDate hoje=LocalDate.of(2026,9,13);
        igual(1L,Regras.calcularStreak(null,hoje,0),"primeira aprovação");
        igual(4L,Regras.calcularStreak(hoje,hoje,4),"mesmo dia mantém streak");
        igual(5L,Regras.calcularStreak(hoje.minusDays(1),hoje,4),"dia consecutivo");
        igual(1L,Regras.calcularStreak(hoje.minusDays(2),hoje,4),"intervalo reinicia streak");
        rejeita(()->Regras.calcularStreak(hoje.plusDays(1),hoje,1),"data futura");
        igual(d("51"),Regras.ajustarReputacao(d("50"),true),"convergência +1");
        igual(d("48"),Regras.ajustarReputacao(d("50"),false),"divergência -2");
        igual(d("100"),Regras.ajustarReputacao(d("100"),true),"teto reputação");
        igual(d("0"),Regras.ajustarReputacao(d("1"),false),"piso reputação");
        Regras.verificarLimite(4,5);testes++;System.out.println("OK limite permite última ação");
        rejeita(()->Regras.verificarLimite(5,5),"limite atingido");
        rejeita(()->Regras.texto(" ","Evidência",500),"evidência vazia");
        rejeita(()->Regras.texto("x".repeat(501),"Evidência",500),"evidência longa");
        char[] senha="HeavenTeste2026!".toCharArray();String hash=Senhas.criar(senha);
        igual(true,Senhas.conferir(senha,hash),"senha correta");
        igual(false,Senhas.conferir("outraSenha".toCharArray(),hash),"senha incorreta");
        igual(false,hash.equals(Senhas.criar(senha)),"salt aleatório");
        igual(false,Senhas.conferir(senha,"valor-inválido"),"hash inválido");
        rejeita(()->Senhas.criar("curta".toCharArray()),"senha curta");Arrays.fill(senha,'\0');
        Usuario u=new Usuario();u.setNome("Teste");igual("Teste",u.getNome(),"encapsulamento modelo");
        igual("USUARIO",Usuario.class.getAnnotation(Tabela.class).value(),"mapeamento tabela");
        System.out.println("TOTAL: "+testes+" testes aprovados. Sem conexão Oracle.");
    }
}
