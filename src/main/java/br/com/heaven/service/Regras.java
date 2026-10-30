package br.com.heaven.service;
import java.math.*;
import java.time.LocalDate;
/** Regras puras: utilizadas pelos BOs e verificadas sem banco de dados. */
public final class Regras {
    private Regras() {}
    public static BigDecimal calcularImpacto(BigDecimal quantidade,BigDecimal fator) {
        if(quantidade==null || fator==null || quantidade.signum()<=0 || fator.signum()<=0)
            throw new RegraNegocioException(CodigoErro.VALIDACAO,"Quantidade e fator devem ser positivos.");
        if(quantidade.stripTrailingZeros().scale()>2 || quantidade.compareTo(new BigDecimal("99999999.99"))>0)
            throw new RegraNegocioException(CodigoErro.VALIDACAO,"Quantidade aceita até duas casas decimais e 99999999,99.");
        BigDecimal impacto=quantidade.multiply(fator).setScale(2,RoundingMode.HALF_UP);
        if(impacto.compareTo(new BigDecimal("9999999999.99"))>0) throw new RegraNegocioException(CodigoErro.VALIDACAO,"Impacto excede a capacidade do banco.");
        return impacto;
    }
    public static long calcularPontos(BigDecimal impacto,long pontosBase) {
        if(impacto==null || impacto.signum()<=0 || pontosBase<=0) throw new RegraNegocioException(CodigoErro.REGRA,"Impacto e pontos base devem ser positivos.");
        BigDecimal pontos=impacto.multiply(BigDecimal.valueOf(pontosBase)).setScale(0,RoundingMode.HALF_UP).max(BigDecimal.ONE);
        if(pontos.compareTo(new BigDecimal("99999999"))>0) throw new RegraNegocioException(CodigoErro.REGRA,"Pontuação excede oito dígitos.");
        return pontos.longValueExact();
    }
    public static String decidirResultado(int votos,int minimo,BigDecimal pesoAprovado,BigDecimal pesoTotal,BigDecimal percentual) {
        if(minimo<1 || votos<0 || percentual==null || percentual.signum()<=0 || percentual.compareTo(new BigDecimal("100"))>0)
            throw new RegraNegocioException(CodigoErro.REGRA,"Parâmetros de votação inválidos.");
        if(votos<minimo) return "PENDENTE";
        if(pesoTotal==null || pesoAprovado==null || pesoTotal.signum()<=0 || pesoAprovado.signum()<0 || pesoAprovado.compareTo(pesoTotal)>0)
            throw new RegraNegocioException(CodigoErro.REGRA,"Pesos de votação inválidos.");
        return pesoAprovado.multiply(new BigDecimal("100")).compareTo(pesoTotal.multiply(percentual))>=0?"APROVADA":"REJEITADA";
    }
    public static long calcularStreak(LocalDate ultima,LocalDate hoje,long atual) {
        if(hoje==null || atual<0 || ultima!=null && ultima.isAfter(hoje)) throw new RegraNegocioException(CodigoErro.REGRA,"Datas/streak inválidos.");
        if(ultima==null || ultima.isBefore(hoje.minusDays(1))) return 1;
        return ultima.equals(hoje)?atual:atual+1;
    }
    public static BigDecimal ajustarReputacao(BigDecimal atual,boolean convergiu) {
        if(atual==null || atual.signum()<0 || atual.compareTo(new BigDecimal("100"))>0) throw new RegraNegocioException(CodigoErro.REGRA,"Reputação inválida.");
        return atual.add(BigDecimal.valueOf(convergiu?1:-2)).max(BigDecimal.ZERO).min(new BigDecimal("100"));
    }
    public static void verificarLimite(long feitos,long limite) {
        if(feitos<0 || limite<1 || feitos>=limite) throw new RegraNegocioException(CodigoErro.REGRA,"Limite diário atingido ou inválido.");
    }
    public static String texto(String valor,String campo,int max) {
        if(valor==null || valor.isBlank() || valor.strip().length()>max) throw new RegraNegocioException(CodigoErro.VALIDACAO,campo+" é obrigatório (até "+max+" caracteres).");
        return valor.strip();
    }
    /** Texto opcional: vazio vira null; acima do limite é erro de validação. */
    public static String textoOpcional(String valor,String campo,int max) {
        if(valor==null || valor.isBlank()) return null;
        if(valor.strip().length()>max) throw new RegraNegocioException(CodigoErro.VALIDACAO,campo+" aceita até "+max+" caracteres.");
        return valor.strip();
    }
}
