package br.com.heaven.service;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.*;
import java.util.*;
public final class Senhas {
    private static final int ITERACOES=210000;
    private Senhas() {}
    public static String criar(char[] senha) {
        if(senha==null || senha.length<8) throw new RegraNegocioException(CodigoErro.VALIDACAO,"Senha deve ter ao menos 8 caracteres.");
        byte[] salt=new byte[16]; new SecureRandom().nextBytes(salt);
        return "pbkdf2$"+ITERACOES+"$"+Base64.getEncoder().encodeToString(salt)+"$"+Base64.getEncoder().encodeToString(derivar(senha,salt,ITERACOES));
    }
    public static boolean conferir(char[] senha,String hash) {
        if(senha==null || hash==null) return false;
        try {
            String[] p=hash.split("\\$");
            if(p.length!=4 || !p[0].equals("pbkdf2")) return false;
            int n=Integer.parseInt(p[1]); if(n<10000 || n>1000000) return false;
            return MessageDigest.isEqual(Base64.getDecoder().decode(p[3]),derivar(senha,Base64.getDecoder().decode(p[2]),n));
        } catch(IllegalArgumentException e) { return false; }
    }
    private static byte[] derivar(char[] senha,byte[] salt,int n) {
        PBEKeySpec spec=new PBEKeySpec(senha,salt,n,256);
        try { return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded(); }
        catch(GeneralSecurityException e) { throw new IllegalStateException(e); }
        finally { spec.clearPassword(); }
    }
}
